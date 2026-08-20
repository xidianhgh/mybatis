package com.ruijie.listenevent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 服务：负责文档加载、向量检索、Prompt 增强
 *
 * 执行链路：用户提问 → 强制触发向量检索 → 获取 TopN 片段 → 片段+问题塞进 Prompt → 调用 LLM
 *
 * 向量存储已切换为 Milvus 数据库（由 spring-ai-starter-vector-store-milvus 自动配置）
 * 原 SimpleVectorStore（内存版）已替换，配置见 application.yml: spring.ai.vectorstore.milus
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    // 原 SimpleVectorStore（内存向量存储）已替换为 MilvusVectorStore（Milvus 向量数据库）
    // VectorStore 接口不变，Spring 自动注入 MilvusVectorStore 实现
    @Autowired
    private VectorStore vectorStore;

    /** 用于 LLM 重排的 ChatModel */
    private final ChatModel chatModel;

    public RagService(@Qualifier("ollamaChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /** RAG 检索返回的文档片段数量 */
    @Value("${spring.ai.rag.top-k:3}")
    private int topK;

    /** 相似度阈值：低于此分数的结果视为不相关，触发自动跳过 RAG */
    @Value("${spring.ai.rag.similarity-threshold:0.5}")
    private double similarityThreshold;

    /** 每个 chunk 的目标 token 数 */
    @Value("${spring.ai.rag.chunk-size:800}")
    private int chunkSize;

    /** 相邻 chunk 之间的重叠 token 数，防止语义割裂 */
    @Value("${spring.ai.rag.chunk-overlap:200}")
    private int chunkOverlap;

    /** 重排时召回的候选文档倍数（相对于 topK） */
    @Value("${spring.ai.rag.rerank-candidate-multiplier:3}")
    private int rerankCandidateMultiplier;

    /** 重排后最终保留的文档数量（默认等于 topK） 如果不希望重排，可以把这个值设大一点*/
    @Value("${spring.ai.rag.rerank-top-k:${spring.ai.rag.top-k:3}}")
    private int rerankTopK;

    /**
     * 加载文档到向量存储（使用 TokenTextSplitter 按 token 分块 + overlap 重叠）
     * 支持 txt 等纯文本文件，如需 PDF/Word 可换用 TikaDocumentReader
     *
     * 分块策略：
     * 1. TextReader 读取整个文件为原始 Document
     * 2. TokenTextSplitter 按 token 数切分，并通过 overlap 保持上下文连贯
     * 3. 将分块后的 Document 存入向量库
     *
     * @param resource Spring Resource 对象
     * @return 加载的文档片段数量
     */
    public int loadDocument(Resource resource) {
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("source", resource.getFilename());
        List<Document> rawDocuments = textReader.get();

        // 使用 TokenTextSplitter 进行分块
        // 参数：chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, separators
        TokenTextSplitter splitter = new TokenTextSplitter(
                chunkSize,       // 每个 chunk 的目标 token 数
                350,             // 最小 chunk 字符数，低于此值不再切分
                5,               // 小于此长度的 chunk 不生成向量
                chunkOverlap,    // 相邻 chunk 重叠 token 数，保持语义连贯
                true,            // 保留分隔符
                List.of('\n')    // 分隔符列表
        );
        List<Document> chunkedDocuments = splitter.apply(rawDocuments);

        vectorStore.add(chunkedDocuments);
        return chunkedDocuments.size();
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
     * 带重排的向量检索：先召回更多候选，再用 LLM 打分重排，最终取 topK
     * 重排流程：
     * 1. 从 Milvus 召回 topK * rerankCandidateMultiplier 个候选文档
     * 2. 用 LLM 对每个文档与 query 的相关性打分（0-10）
     * 3. 按分数降序排序，取前 rerankTopK 个
     *
     * @param query 用户问题
     * @return 重排后的相关文档内容列表
     */
    public List<String> searchWithRerank(String query) {
        int candidateCount = topK * rerankCandidateMultiplier;
        // 第一步：粗召回 —— 从向量库召回更多候选文档
        List<Document> candidates = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(candidateCount)
                        .build()
        );
        if (candidates.isEmpty()) {
            return List.of();
        }
        // 如果候选数不超过最终保留数，无需重排
        if (candidates.size() <= rerankTopK) {
            return candidates.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());
        }
        // 第二步：LLM 重排 —— 对每个候选文档打分
        List<ScoredDocument> scoredDocs = new ArrayList<>();
        for (Document doc : candidates) {
            double score = rerankScore(query, doc.getText());
            scoredDocs.add(new ScoredDocument(doc, score));
            log.debug("Rerank score={:.2f} | doc={}", score,
                    doc.getText().substring(0, Math.min(80, doc.getText().length())));
        }
        // 第三步：按分数降序排序，取 topK
        scoredDocs.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
        return scoredDocs.stream()
                .limit(rerankTopK)
                .map(sd -> sd.doc().getText())
                .collect(Collectors.toList());
    }

    /**
     * 带相关性过滤 + 重排的向量检索：先召回、再重排、最后过滤低分
     *
     * @param query 用户问题
     * @return 高相关且重排后的文档内容列表（可能为空）
     */
    public List<String> searchWithRelevanceAndRerank(String query) {
        int candidateCount = topK * rerankCandidateMultiplier;
        List<Document> candidates = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(candidateCount)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );
        if (candidates.isEmpty()) {
            return List.of();
        }
        if (candidates.size() <= rerankTopK) {
            return candidates.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());
        }
        // LLM 重排打分
        List<ScoredDocument> scoredDocs = new ArrayList<>();
        for (Document doc : candidates) {
            double score = rerankScore(query, doc.getText());
            scoredDocs.add(new ScoredDocument(doc, score));
        }
        scoredDocs.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
        // 过滤掉重排分数过低的文档（阈值 5 分，满分 10）
        return scoredDocs.stream()
                .limit(rerankTopK)
                .filter(sd -> sd.score() >= 5.0)
                .map(sd -> sd.doc().getText())
                .collect(Collectors.toList());
    }

    /**
     * 使用 LLM 对单个文档进行相关性打分
     * 让 LLM 判断文档与问题的相关程度，返回 0-10 分
     *
     * @param query    用户问题
     * @param docText  文档内容
     * @return 相关性分数（0.0 - 10.0）
     */
    private double rerankScore(String query, String docText) {
        String promptText = """
                请判断以下文档片段与用户问题的相关程度。
                只回复一个 0 到 10 之间的整数，不要回复任何其他内容。
                0 表示完全不相关，10 表示完全相关。

                用户问题：%s

                文档片段：%s

                相关性分数（0-10）：
                """.formatted(query, docText);

        try {
            String response = chatModel.call(new Prompt(promptText))
                    .getResult().getOutput().getText();
            // 提取数字（LLM 可能回复 "7" 或 "相关性：7" 等格式）
            String trimmed = response.trim().replaceAll("[^0-9.]", "");
            if (trimmed.isEmpty()) {
                return 0.0;
            }
            double score = Double.parseDouble(trimmed);
            return Math.min(10.0, Math.max(0.0, score));
        } catch (Exception e) {
            log.warn("Rerank scoring failed for doc, returning 0. Error: {}", e.getMessage());
            return 0.0;
        }
    }

    /** 带分数的文档记录，用于重排排序 */
    private record ScoredDocument(Document doc, double score) {}

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
