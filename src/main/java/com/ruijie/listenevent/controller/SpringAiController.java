package com.ruijie.listenevent.controller;

import com.alibaba.fastjson.JSONObject;
import com.ruijie.listenevent.service.LongTermMemoryService;
import com.ruijie.listenevent.service.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
public class SpringAiController {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private MessageWindowChatMemory chatMemory;

    @Autowired
    private RagService ragService;

    @Autowired
    private LongTermMemoryService longTermMemoryService;

    /**
     * 同步多轮对话接口 —— 集成短期记忆（Redis）+ 长期记忆（Milvus）+ 按需 RAG
     * POST http://localhost:9999/ai/chat
     * Body: {"msg": "你好", "conversationId": "可选", "needRag": true/false/不传}
     *
     * 记忆架构：
     * - 短期记忆（Redis）：当前会话的对话上下文，自动通过 MessageWindowChatMemory 管理
     * - 长期记忆（Milvus）：跨会话的用户偏好/重要事实，自动召回并注入 prompt
     * - RAG（Milvus）：知识库文档检索，按需触发
     *
     * 执行链路：
     * 1. 召回长期记忆 → 注入 system prompt
     * 2. 按需 RAG 检索 → 追加到 system prompt
     * 3. 短期记忆自动加载（通过 MessageChatMemoryAdvisor）
     * 4. 调用 LLM 生成回复
     * 5. 异步提取并存储长期记忆
     */
    @PostMapping("/ai/chat")
    public JSONObject chat(@RequestBody JSONObject req) {
        String msg = req.getString("msg");
        String conversationId = req.getString("conversationId");
        Boolean needRag = req.getBoolean("needRag");
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        // ====== 长期记忆召回：跨会话的用户偏好和重要事实 ======
        String memoryContext = longTermMemoryService.buildMemoryContext(msg, 5);

        // ====== 按需 RAG：根据 needRag 参数决定策略（带 LLM 重排） ======
        String ragContext = null;
        if (Boolean.TRUE.equals(needRag)) {
            List<String> contextParts = ragService.searchWithRerank(msg);
            ragContext = ragService.buildEnhancedSystemPrompt(msg, contextParts);
        } else if (!Boolean.FALSE.equals(needRag)) {
            List<String> relevantParts = ragService.searchWithRelevanceAndRerank(msg);
            if (!relevantParts.isEmpty()) {
                ragContext = ragService.buildEnhancedSystemPrompt(msg, relevantParts);
            }
        }

        // ====== 合并 system prompt：长期记忆 + RAG 上下文 ======
        String combinedSystemPrompt = combinePrompts(memoryContext, ragContext);

        // ====== 构建 ChatClient 调用（短期记忆通过 Advisor 自动加载） ======
        var promptSpec = chatClient.prompt()
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build());

        if (combinedSystemPrompt != null) {
            promptSpec = promptSpec.system(combinedSystemPrompt);
        }

        String reply = promptSpec.user(msg)
                .call()
                .content();

        // ====== 异步提取并存储长期记忆（不阻塞主流程） ======
        longTermMemoryService.extractAndStoreMemory(msg, reply);

        JSONObject result = new JSONObject();
        result.put("reply", reply);
        result.put("conversationId", conversationId);
        return result;
    }

    /**
     * 合并长期记忆和 RAG 上下文为统一的 system prompt
     */
    private String combinePrompts(String memoryContext, String ragContext) {
        if (memoryContext == null && ragContext == null) return null;
        StringBuilder sb = new StringBuilder();
        if (memoryContext != null) {
            sb.append(memoryContext).append("\n\n");
        }
        if (ragContext != null) {
            sb.append(ragContext);
        }
        return sb.toString();
    }

    /**
     * 流式多轮对话接口（SSE）—— 集成 RAG
     * POST http://localhost:9999/ai/stream-chat
     * Body: {"msg": "你好", "conversationId": "可选，不传则自动生成"}
     *
     * 执行链路：
     * 1. 用户提问 → 程序固定触发向量检索
     * 2. 拿到 TopN 片段
     * 3. 把片段 + 问题一起塞进 Prompt（system 消息携带参考资料）
     * 4. 调用 LLM 流式生成
     * 5. Prompt 约束：如果参考资料里没有答案，如实说无法回答，不要编造
     */
//    @PostMapping(value="/ai/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PostMapping("/ai/stream-chat")
    public SseEmitter streamChat(@RequestBody JSONObject req) {
        String msg = req.getString("msg");
        String conversationId = req.getString("conversationId");
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        SseEmitter emitter = new SseEmitter(120_000L);
        final String cid = conversationId;

        new Thread(() -> {
            try {
                // ====== RAG 步骤：强制先走向量检索 ======
                List<String> contextParts = ragService.search(msg);
                String systemPrompt = ragService.buildEnhancedSystemPrompt(msg, contextParts);

                // ====== 构建 ChatClient 调用 ======
                var promptSpec = chatClient.prompt()
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                                .conversationId(cid)
                                .build());

                // 如果有 RAG 上下文，注入 system 提示词
                if (systemPrompt != null) {
                    promptSpec = promptSpec.system(systemPrompt);
                }

                promptSpec.user(msg)
                        .stream()
                        .content()
                        .doOnNext(text -> {
                            if (text != null && !text.isEmpty()) {
                                try {
                                    emitter.send(SseEmitter.event().data(text));
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            }
                        })
                        .doOnError(emitter::completeWithError)
                        .doOnComplete(emitter::complete)
                        .subscribe();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 文档上传接口：将文档加载到向量存储中，供 RAG 检索使用
     * POST http://localhost:9999/ai/rag/upload
     * Content-Type: multipart/form-data
     * 参数: file - 上传的文件（支持 txt 等纯文本格式）
     */
    @PostMapping("/ai/rag/upload")
    public JSONObject uploadDocument(@RequestParam("file") MultipartFile file) {
        JSONObject result = new JSONObject();
        try {
            int chunks = ragService.loadDocument(new InputStreamResource(file.getInputStream()));
            result.put("success", true);
            result.put("fileName", file.getOriginalFilename());
            result.put("chunks", chunks);
            result.put("message", "文档已成功加载到向量存储");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "文档加载失败: " + e.getMessage());
        }
        return result;
    }
}
