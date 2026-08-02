package com.ruijie.listenevent.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 服务：负责文档加载、向量检索、Prompt 增强
 *
 * 执行链路：用户提问 → 强制触发向量检索 → 获取 TopN 片段 → 片段+问题塞进 Prompt → 调用 LLM
 */
@Service
public class RagService {

    @Autowired
    private VectorStore vectorStore;

    /** RAG 检索返回的文档片段数量 */
    @Value("${spring.ai.rag.top-k:3}")
    private int topK;

    /** 相似度阈值：低于此分数的结果视为不相关，触发自动跳过 RAG */
    @Value("${spring.ai.rag.similarity-threshold:0.5}")
    private double similarityThreshold;

    /**
     * 加载文档到向量存储
     * 支持 txt 等纯文本文件，如需 PDF/Word 可换用 TikaDocumentReader
     *
     * @param resource Spring Resource 对象
     * @return 加载的文档片段数量
     */
    public int loadDocument(Resource resource) {
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("source", resource.getFilename());
        List<Document> documents = textReader.get();
        vectorStore.add(documents);
        return documents.size();
    }

    /**
     * 向量检索：根据用户问题搜索最相关的 TopN 文档片段
     *
     * @param query 用户问题
     * @return 相关文档内容列表
     */
    public List<String> search(String query) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build()
        );
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }

    /**
     * 带相关性过滤的向量检索：只返回相似度高于阈值的文档片段
     * 用于自动判断是否需要 RAG —— 如果没有高相关结果，说明问题不依赖知识库
     *
     * @param query 用户问题
     * @return 高相关的文档内容列表（可能为空，表示不需要 RAG）
     */
    public List<String> searchWithRelevance(String query) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }

    /**
     * 构建增强 Prompt：将参考资料 + 用户问题 + 约束规则合并
     *
     * 约束规则：如果参考资料里没有答案，如实说无法回答，不要编造
     *
     * @param question     用户原始问题
     * @param contextParts 检索到的文档片段
     * @return 增强后的系统提示词（null 表示无参考资料，不需要 RAG 增强）
     */
    public String buildEnhancedSystemPrompt(String question, List<String> contextParts) {
        if (contextParts == null || contextParts.isEmpty()) {
            return null;
        }

        String context = String.join("\n---\n", contextParts);

        return """
                你是一个严谨的问答助手。请严格基于以下参考资料回答用户的问题。

                【参考资料】
                %s

                【回答规则】
                1. 只根据上述参考资料中的信息来回答，不要添加任何参考资料中没有的内容。
                2. 如果参考资料中没有相关信息，或者信息不足以回答问题，请如实回答："根据已有参考资料，我无法回答该问题。"
                3. 绝对不要编造、猜测或杜撰任何信息。
                4. 回答时尽量引用参考资料中的原文。
                """.formatted(context);
    }
}
