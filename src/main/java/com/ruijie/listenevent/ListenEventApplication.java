package com.ruijie.listenevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration;

// 排除 MCP 自动配置，避免 MCP Server 连接失败时拖垮整个应用启动
//@SpringBootApplication(exclude = {
//        McpClientAutoConfiguration.class,
//        McpToolCallbackAutoConfiguration.class
//})
@SpringBootApplication
public class ListenEventApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListenEventApplication.class, args);
        System.out.println("启动成功");
    }

}
