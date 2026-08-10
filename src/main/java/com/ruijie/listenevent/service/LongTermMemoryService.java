package com.ruijie.listenevent.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 长期记忆服务：使用独立的 Milvus 集合存储用户跨会话的持久记忆
 *
 * 存储内容：
 * - 用户持久偏好（如：喜欢简洁回答、偏好中文回复）
 * - 历史重要事实（如：用户是Java开发者、在某某公司工作）
 * - 长期任务信息（如：正在做XX项目、下周有XX会议）
 * - 跨会话知识（脱离单次 session，下次新开对话依然可以召回）
 *
 * 兜底策略：
 * - Milvus 可用时：正常存取长期记忆
 * - Milvus 不可用时：跳过长期记忆，不影响正常对话（降级为空记忆）
 */
@Service
public class LongTermMemoryService {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryService.class);

    /** 长期记忆专用的 Milvus 集合名称（与 RAG 集合 hgh 隔离） */
    private static final String LTM_COLLECTION = "long_term_memory";

    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;
    private final VectorStore longTermVectorStore;
    private volatile boolean available = true;

    @Value("${spring.ai.vectorstore.milvus.client.host:127.0.0.1}")
    private String milvusHost;

    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    private int milvusPort;

    public LongTermMemoryService(
            EmbeddingModel embeddingModel,
            @Qualifier("ollamaChatModel") ChatModel chatModel) {
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
        this.longTermVectorStore = initLongTermVectorStore();
    }

    /**
     * 初始化长期记忆专用的 Milvus VectorStore（独立集合，与 RAG 的 hgh 集合隔离）
     * 使用 MilvusVectorStore.builder() 创建，避免与自动配置的 RAG VectorStore 冲突
     */
    private VectorStore initLongTermVectorStore() {
        try {
            MilvusServiceClient client = new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            .withHost(milvusHost)
                            .withPort(milvusPort)
                            .build()
            );

            // 使用 Builder 模式创建独立的 MilvusVectorStore
            MilvusVectorStore store = MilvusVectorStore.builder(client, embeddingModel)
                    .collectionName(LTM_COLLECTION)
                    .initializeSchema(true)
                    .build();

            // 健康检查
            store.similaritySearch(SearchRequest.builder().query("health_check").topK(1).build());
            available = true;
            log.info("长期记忆 Milvus 集合 [{}] 初始化成功", LTM_COLLECTION);
            return store;
        } catch (Exception e) {
            available = false;
            log.warn("长期记忆 Milvus 初始化失败，长期记忆功能降级为空。错误: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 存储长期记忆到 Milvus
     *
     * @param text     记忆文本内容
     * @param category 记忆类别（preference/fact/task/knowledge）
     */
    public void storeMemory(String text, String category) {
        if (!available || longTermVectorStore == null) return;
        try {
            Document doc = new Document(text, Map.of(
                    "memory_type", "long_term",
                    "category", category,
                    "created_at", System.currentTimeMillis()
            ));
            longTermVectorStore.add(List.of(doc));
            log.debug("长期记忆已存储 [{}]: {}", category, text.substring(0, Math.min(60, text.length())));
        } catch (Exception e) {
            log.warn("长期记忆存储失败: {}", e.getMessage());
        }
    }

    /**
     * 从对话中提取重要信息并存储为长期记忆（异步执行，不阻塞主流程）
     *
     * @param userQuery     用户原始问题
     * @param assistantReply AI 回复内容
     */
    @Async
    public void extractAndStoreMemory(String userQuery, String assistantReply) {
        if (!available) return;
        try {
            String extractionPrompt = """
                    请从以下对话中提取值得长期记住的信息，包括：
                    1. 用户偏好（如喜欢简洁回答、偏好某种语言等）
                    2. 关于用户的重要事实（如职业、公司、技能等）
                    3. 长期任务或项目信息
                    4. 跨会话有用的知识

                    如果没有值得记住的信息，回复：无

                    如果有，每行一条，格式：类别|内容
                    类别可选：preference/fact/task/knowledge

                    用户：%s
                    助手：%s
                    """.formatted(userQuery, assistantReply);

            String response = chatModel.call(new Prompt(extractionPrompt))
                    .getResult().getOutput().getText();

            if (response == null || response.isBlank() || response.contains("无")) return;

            String[] lines = response.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    String category = parts[0].trim();
                    String content = parts[1].trim();
                    if (!content.isEmpty()) {
                        storeMemory(content, category);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取长期记忆失败: {}", e.getMessage());
        }
    }

    /**
     * 根据用户问题召回相关的长期记忆
     *
     * @param query 用户问题
     * @param topK  召回数量
     * @return 相关记忆文本列表（Milvus 不可用时返回空列表）
     */
    public List<String> recallMemories(String query, int topK) {
        if (!available || longTermVectorStore == null) return List.of();
        try {
            List<Document> docs = longTermVectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .build()
            );
            return docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("长期记忆召回失败，降级为空记忆: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 构建长期记忆上下文文本（注入到 system prompt 中）
     *
     * @param query     用户问题
     * @param maxItems  最多召回几条记忆
     * @return 格式化的记忆上下文（无记忆时返回 null）
     */
    public String buildMemoryContext(String query, int maxItems) {
        List<String> memories = recallMemories(query, maxItems);
        if (memories.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("【用户长期记忆（以下是该用户的历史偏好和重要信息，请参考但不要主动提及）】\n");
        for (int i = 0; i < memories.size(); i++) {
            sb.append("- ").append(memories.get(i)).append("\n");
        }
        return sb.toString();
    }

    /** 长期记忆服务是否可用 */
    public boolean isAvailable() {
        return available;
    }
}
