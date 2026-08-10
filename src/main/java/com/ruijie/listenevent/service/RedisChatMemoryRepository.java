package com.ruijie.listenevent.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的短期记忆存储（ChatMemoryRepository 实现）
 *
 * 兜底策略：
 * - Redis 可用时：使用 Redis 存储，支持分布式、持久化、TTL 自动过期
 * - Redis 不可用时：降级到本地内存（ConcurrentHashMap），保证服务可用
 *
 * Key 设计：chat:memory:{conversationId}
 * 数据类型：Redis List（每个元素为一条消息的 JSON）
 * TTL：默认 30 分钟，每次写入时刷新
 */
@Service
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryRepository.class);

    private static final String KEY_PREFIX = "chat:memory:";
    private static final int MAX_MESSAGES = 50;
    private static final long TTL_MINUTES = 30;

    private final StringRedisTemplate redisTemplate;

    /** 本地兜底存储：Redis 不可用时使用 */
    private final Map<String, List<Message>> fallbackStore = new ConcurrentHashMap<>();

    /** Redis 可用性标记（volatile 保证可见性） */
    private volatile boolean redisAvailable = true;

    public RedisChatMemoryRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 启动时探测 Redis 连通性
        checkRedisAvailability();
    }

    @Override
    public List<String> findConversationIds() {
        try {
            if (!redisAvailable) return new ArrayList<>(fallbackStore.keySet());
            Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) return List.of();
            return keys.stream().map(k -> k.substring(KEY_PREFIX.length())).toList();
        } catch (Exception e) {
            log.warn("Redis不可用，降级到本地内存查询会话列表", e);
            redisAvailable = false;
            return new ArrayList<>(fallbackStore.keySet());
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        try {
            if (!redisAvailable) return fallbackStore.getOrDefault(conversationId, List.of());
            List<String> rawList = redisTemplate.opsForList().range(key, 0, -1);
            if (rawList == null || rawList.isEmpty()) return List.of();
            return rawList.stream().map(this::deserialize).filter(Objects::nonNull).toList();
        } catch (Exception e) {
            log.warn("Redis不可用，降级到本地内存读取会话 {}", conversationId, e);
            redisAvailable = false;
            return fallbackStore.getOrDefault(conversationId, List.of());
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        try {
            if (!redisAvailable) {
                fallbackStore.put(conversationId, new ArrayList<>(messages));
                return;
            }
            // 先清除旧数据，再写入全部
            redisTemplate.delete(key);
            for (Message msg : messages) {
                redisTemplate.opsForList().rightPush(key, serialize(msg));
            }
            // 限制最大消息数（保留最新的 N 条）
            redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
            // 刷新 TTL
            redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis不可用，降级到本地内存写入会话 {}", conversationId, e);
            redisAvailable = false;
            fallbackStore.put(conversationId, new ArrayList<>(messages));
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        try {
            if (!redisAvailable) {
                fallbackStore.remove(conversationId);
                return;
            }
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis不可用，降级到本地内存清除会话 {}", conversationId, e);
            redisAvailable = false;
            fallbackStore.remove(conversationId);
        }
    }

    /**
     * 启动时检查 Redis 连通性
     */
    private void checkRedisAvailability() {
        try {
            redisTemplate.opsForList().size("__health_check__");
            redisAvailable = true;
            log.info("Redis 连接正常，短期记忆使用 Redis 存储");
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis 连接失败，短期记忆降级到本地内存存储。错误: {}", e.getMessage());
        }
    }

    // ====== 序列化/反序列化 ======

    private String serialize(Message message) {
        JSONObject json = new JSONObject();
        json.put("messageType", message.getMessageType().name());
        json.put("text", message.getText());
        return json.toJSONString();
    }

    private Message deserialize(String json) {
        try {
            JSONObject obj = JSON.parseObject(json);
            String type = obj.getString("messageType");
            String text = obj.getString("text");
            return switch (MessageType.valueOf(type)) {
                case USER -> new UserMessage(text);
                case ASSISTANT -> new AssistantMessage(text);
                case SYSTEM -> new SystemMessage(text);
                default -> new UserMessage(text);
            };
        } catch (Exception e) {
            log.warn("消息反序列化失败: {}", json, e);
            return null;
        }
    }
}
