package com.ruijie.listenevent.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
                        .disableThinking()   // 关闭思考模式，确保模型正确触发工具调用
                        .build())
                .build();
    }

    /**
     * ChatClient：封装了 ChatModel + 对话记忆 Advisor + MCP 工具
     * MCP 工具通过 ToolCallbackProvider 自动注入，AI 可在对话中调用 Brave Search 和 FileSystem
     *
     * 注意：chatMemory Bean 已迁移到 MemoryConfig 中（使用 Redis 存储，支持兜底）
     */
    @Bean
    public ChatClient chatClient(OllamaChatModel ollamaChatModel, MessageWindowChatMemory chatMemory,
                                 @Autowired(required = false) ToolCallbackProvider[] mcpTools) {
        var builder = ChatClient.builder(ollamaChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());

        // 如果有 MCP 工具（Client + Server），全部注入到 ChatClient
        if (mcpTools != null) {
            java.util.List<String> allToolNames = new java.util.ArrayList<>();
            for (ToolCallbackProvider provider : mcpTools) {
                builder.defaultToolCallbacks(provider.getToolCallbacks());
                // 收集所有工具名称，用于显式启用（defaultToolCallbacks 注册但默认不启用）
                for (ToolCallback cb : provider.getToolCallbacks()) {
                    allToolNames.add(cb.getToolDefinition().name());
                }
            }
            // 显式启用所有工具，使其在每次请求中可用
            if (!allToolNames.isEmpty()) {
                builder.defaultToolNames(allToolNames.toArray(new String[0]));
            }
        }

        return builder.build();
    }
}
