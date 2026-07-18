package com.ruijie.listenevent.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.model:mydeep}")
    private String model;

    @Value("${spring.ai.ollama.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.ollama.options.num-ctx:2048}")
    private Integer numCtx;

    @Value("${spring.ai.ollama.options.top-p:0.95}")
    private Double topP;

    @Bean
    public OllamaChatModel ollamaChatModel() {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .numCtx(numCtx)
                        .topP(topP)
                        .build())
                .build();
    }

    /**
     * 对话记忆存储（内存版，最多保存20条历史消息）
     */
    @Bean
    public MessageWindowChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    /**
     * ChatClient：封装了 ChatModel + 对话记忆 Advisor
     */
    @Bean
    public ChatClient chatClient(OllamaChatModel ollamaChatModel, MessageWindowChatMemory chatMemory) {
        return ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
