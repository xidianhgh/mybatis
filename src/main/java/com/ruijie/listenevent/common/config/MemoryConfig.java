package com.ruijie.listenevent.common.config;

import com.ruijie.listenevent.service.RedisChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 记忆配置：短期记忆（Redis）+ 异步支持
 *
 * 短期记忆通过 RedisChatMemoryRepository 存储到 Redis，Redis 不可用时自动降级到本地内存
 * 长期记忆由 LongTermMemoryService 独立管理，使用独立的 Milvus 集合
 */
@Configuration
@EnableAsync
public class MemoryConfig {

    /**
     * 短期记忆：基于 Redis 的对话记忆（最多保留 20 条消息窗口）
     * Redis 挂了自动降级到本地内存，保证服务可用
     */
    @Bean
    public MessageWindowChatMemory chatMemory(RedisChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(20)
                .build();
    }
}
