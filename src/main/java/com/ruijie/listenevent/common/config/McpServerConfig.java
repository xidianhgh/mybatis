package com.ruijie.listenevent.common.config;

import com.ruijie.listenevent.service.mcp.AiChatToolService;
import com.ruijie.listenevent.service.mcp.WeatherToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 工具注册配置
 * 将带有 @Tool 注解的方法注册为 MCP 工具，通过 Streamable-HTTP 端点对外暴露
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            WeatherToolService weatherToolService,
            AiChatToolService aiChatToolService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherToolService, aiChatToolService)
                .build();
    }
}
