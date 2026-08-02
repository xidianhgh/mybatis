package com.ruijie.listenevent.common.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 相关配置：EmbeddingModel + VectorStore
 */
@Configuration
public class RagConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.rag.embedding-model:nomic-embed-text}")
    private String embeddingModel;

    @Bean
    public OllamaApi ollamaApi() {
        return OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 向量化模型：用于将文本转为向量（需要先在 Ollama 中拉取对应模型）
     * 拉取命令：ollama pull nomic-embed-text
     */
    @Bean
    public EmbeddingModel embeddingModel(OllamaApi ollamaApi) {
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model(embeddingModel)
                .build();
        return new OllamaEmbeddingModel(ollamaApi, options,
                ObservationRegistry.NOOP, ModelManagementOptions.defaults());
    }

    /**
     * 内存向量存储：用于存储文档向量并进行相似度检索
     * 生产环境可替换为 Milvus/Redis/PG 等持久化向量数据库
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
