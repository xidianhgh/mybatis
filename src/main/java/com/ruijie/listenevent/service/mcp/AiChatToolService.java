package com.ruijie.listenevent.service.mcp;

import com.ruijie.listenevent.service.RagService;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP Tool：AI 智能对话工具（集成 RAG 知识库检索）
 * 通过 MCP Server 以 Streamable-HTTP 模式对外暴露
 *
 * 注意：此处使用 ChatModel 而非 ChatClient，避免与 MCP Server 工具注册产生循环依赖
 */
@Service
public class AiChatToolService {

    private final ChatModel chatModel;
    private final RagService ragService;

    public AiChatToolService(@Qualifier("ollamaChatModel") ChatModel chatModel, RagService ragService) {
        this.chatModel = chatModel;
        this.ragService = ragService;
    }

    @Tool(description = "AI 智能对话：基于知识库进行问答。输入用户问题，自动检索知识库并生成回答。" +
            "如果知识库中没有相关信息，会如实告知无法回答。")
    public String chatWithKnowledgeBase(
            @ToolParam(description = "用户的问题") String question) {

        // 先进行 RAG 向量检索
        List<String> relevantParts = ragService.searchWithRelevance(question);

        // 构建消息列表
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();
        if (!relevantParts.isEmpty()) {
            String systemPrompt = ragService.buildEnhancedSystemPrompt(question, relevantParts);
            messages.add(new SystemMessage(systemPrompt));
        }
        messages.add(new UserMessage(question));

        return chatModel.call(new Prompt(messages)).getResult().getOutput().getText();
    }

    @Tool(description = "知识库向量检索：根据关键词搜索知识库中的相关文档片段，返回最匹配的文档内容。" +
            "适用于需要查找特定信息的场景。")
    public String searchKnowledgeBase(
            @ToolParam(description = "搜索关键词或问题描述") String query) {

        List<String> results = ragService.search(query);
        if (results.isEmpty()) {
            return "未找到相关知识库内容。";
        }
        return String.join("\n---\n", results);
    }
}
