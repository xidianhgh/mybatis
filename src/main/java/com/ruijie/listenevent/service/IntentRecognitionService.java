package com.ruijie.listenevent.service;

import com.ruijie.listenevent.dto.UserIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 意图识别服务：使用 LLM 对用户输入进行意图分类
 *
 * 识别流程：
 * 1. 构造分类 Prompt（包含意图定义 + 用户消息）
 * 2. 调用 LLM 返回意图标签
 * 3. 解析标签为 UserIntent 枚举（解析失败时降级为 CASUAL_CHAT）
 *
 * 意图类型：
 * - KNOWLEDGE_QA：需要查阅知识库才能回答的专业问题
 * - TOOL_CALL：需要使用外部工具（天气、搜索、文件等）
 * - CASUAL_CHAT：日常问候、闲聊、开放式对话
 * - MEMORY_QUERY：涉及用户个人偏好、历史对话等记忆信息
 */
@Service
public class IntentRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognitionService.class);

    private final ChatModel chatModel;

    public IntentRecognitionService(@Qualifier("ollamaChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 识别用户意图
     *
     * @param userMessage 用户输入消息
     * @return 识别出的意图类型（不会返回 null，解析失败时降级为 CASUAL_CHAT）
     */
    public UserIntent recognize(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return UserIntent.CASUAL_CHAT;
        }

        try {
            String promptText = buildPrompt(userMessage);
            String response = chatModel.call(new Prompt(promptText))
                    .getResult().getOutput().getText();

            UserIntent intent = parseIntent(response);
            log.info("意图识别结果：msg=[{}] intent=[{}]",
                    userMessage.substring(0, Math.min(50, userMessage.length())), intent);
            return intent;
        } catch (Exception e) {
            log.warn("意图识别失败，降级为 CASUAL_CHAT。错误: {}", e.getMessage());
            return UserIntent.CASUAL_CHAT;
        }
    }

    /**
     * 构造意图分类 Prompt
     * 设计原则：
     * - 明确列出每个意图的定义和示例，减少误判
     * - 要求 LLM 只回复标签名，便于解析
     * - 给出兜底策略（无法判断时默认 CASUAL_CHAT）
     */
    private String buildPrompt(String userMessage) {
        return """
                你是一个意图分类器。请根据用户消息判断其意图类型，只回复以下 4 个标签之一，不要回复任何其他内容：

                KNOWLEDGE_QA - 需要查阅专业知识或文档才能回答的问题（如：技术概念解释、业务规则查询、文档内容检索）
                TOOL_CALL - 需要调用外部工具才能完成的任务（如：查天气、搜索网页、读写文件、执行操作）
                CASUAL_CHAT - 日常问候、闲聊、开放式对话、一般性问答（如：你好、谢谢、讲个笑话、常识问答）
                MEMORY_QUERY - 涉及用户自身的偏好、历史、个人信息等需要记忆才能回答的问题（如：我之前说过什么、我喜欢什么）

                分类规则：
                1. 如果问题涉及具体工具的操作（如"今天天气怎么样"、"帮我搜一下"），归为 TOOL_CALL
                2. 如果问题是通用知识或专业概念（如"什么是机器学习"、"解释一下HTTP协议"），归为 KNOWLEDGE_QA
                3. 如果问题涉及用户个人历史信息（如"我上次说了什么"、"我的偏好是什么"），归为 MEMORY_QUERY
                4. 问候、感谢、闲聊、常识问答等简单对话归为 CASUAL_CHAT
                5. 如果无法明确判断，回复 CASUAL_CHAT

                用户消息：%s

                意图标签：
                """.formatted(userMessage);
    }

    /**
     * 解析 LLM 返回的文本为 UserIntent 枚举
     * 支持精确匹配和模糊匹配（LLM 可能回复带空格或大小写不一致）
     *
     * @param response LLM 回复文本
     * @return 对应的 UserIntent，解析失败返回 CASUAL_CHAT
     */
    private UserIntent parseIntent(String response) {
        if (response == null || response.isBlank()) {
            return UserIntent.CASUAL_CHAT;
        }
        String normalized = response.trim().toUpperCase().replaceAll("[^A-Z_]", "");
        try {
            return UserIntent.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // 尝试模糊匹配：检查回复中是否包含意图关键字
            String lower = response.trim().toLowerCase();
            for (UserIntent intent : UserIntent.values()) {
                if (lower.contains(intent.name().toLowerCase())) {
                    return intent;
                }
            }
            log.warn("无法解析意图标签 [{}]，降级为 CASUAL_CHAT", response);
            return UserIntent.CASUAL_CHAT;
        }
    }
}
