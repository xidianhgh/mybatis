package com.ruijie.listenevent.controller;

import com.alibaba.fastjson.JSONObject;
import com.ruijie.listenevent.dto.UserIntent;
import com.ruijie.listenevent.service.IntentRecognitionService;
import com.ruijie.listenevent.service.LongTermMemoryService;
import com.ruijie.listenevent.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(SpringAiController.class);

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private MessageWindowChatMemory chatMemory;

    @Autowired
    private RagService ragService;

    @Autowired
    private LongTermMemoryService longTermMemoryService;

    @Autowired
    private IntentRecognitionService intentRecognitionService;

    /**
     * 同步多轮对话接口 —— 意图识别 + 短期记忆（Redis）+ 长期记忆（Milvus）+ 按需 RAG
     * POST http://localhost:9999/ai/chat
     * Body: {"msg": "你好", "conversationId": "可选", "needRag": true/false/不传, "needIntent": true/false/不传}
     *
     * 参数说明：
     * - needIntent：是否启用意图识别（默认 false）
     *   - true：先做意图识别，根据意图自动决定 RAG 和长期记忆策略
     *   - false/不传：跳过意图识别，走默认链路（长期记忆始终加载，RAG 由 needRag 控制）
     * - needRag：是否启用 RAG（默认 false）
     *   - 开启意图识别后：true 强制 RAG，false 强制跳过，不传由意图决定
     *   - 未开启意图识别时：true 触发 RAG，false/不传跳过 RAG
     *
     * 执行链路（needIntent=true）：
     * 1. 意图识别 → 判断用户意图（KNOWLEDGE_QA / TOOL_CALL / CASUAL_CHAT / MEMORY_QUERY）
     * 2. 根据意图 + needRag 参数，决定处理策略：
     *    - KNOWLEDGE_QA：触发 RAG 检索 + 长期记忆召回
     *    - TOOL_CALL：跳过 RAG 和记忆，直接走工具调用链路
     *    - CASUAL_CHAT：跳过 RAG，轻量对话
     *    - MEMORY_QUERY：触发长期记忆召回，跳过 RAG
     * 3. 合并 system prompt（长期记忆 + RAG 上下文）
     * 4. 短期记忆自动加载（通过 MessageChatMemoryAdvisor）
     * 5. 调用 LLM 生成回复
     * 6. 异步提取并存储长期记忆
     *
     * 执行链路（needIntent=false 或不传）：
     * 1. 始终加载长期记忆 → 注入 system prompt
     * 2. needRag=true 时触发 RAG 检索
     * 3. 短期记忆自动加载
     * 4. 调用 LLM 生成回复
     * 5. 异步提取并存储长期记忆
     */
    @PostMapping("/ai/chat")
    public JSONObject chat(@RequestBody JSONObject req) {
        String msg = req.getString("msg");
        String conversationId = req.getString("conversationId");
        Boolean needRag = req.getBoolean("needRag");
        Boolean needIntent = req.getBoolean("needIntent");
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        // ====== 意图识别（仅在 needIntent=true 时启用） ======
        UserIntent intent = null;
        boolean doRag = false;
        boolean needMemory = false;

        if (Boolean.TRUE.equals(needIntent)) {
            // --- 意图识别模式 ---
            intent = intentRecognitionService.recognize(msg);
            log.info("[意图识别] msg=[{}], intent=[{}]", msg.substring(0, Math.min(50, msg.length())), intent);

            // 是否需要长期记忆：KNOWLEDGE_QA / MEMORY_QUERY 时召回
            needMemory = (intent == UserIntent.KNOWLEDGE_QA || intent == UserIntent.MEMORY_QUERY);
            // 是否需要 RAG：needRag 显式指定时优先，否则由意图决定
            doRag = resolveRagFlag(needRag, intent);
        } else {
            // --- 默认模式：跳过意图识别，长期记忆始终加载，RAG 由 needRag 控制 ---
            needMemory = true;
            doRag = Boolean.TRUE.equals(needRag);
        }

        // ====== 长期记忆召回：跨会话的用户偏好和重要事实 ======
        String memoryContext = null;
        if (needMemory) {
            memoryContext = longTermMemoryService.buildMemoryContext(msg, 5);
        }

        // ====== RAG 检索：根据意图或显式参数触发（带 LLM 重排） ======
        String ragContext = null;
        if (doRag) {
            List<String> contextParts = ragService.searchWithRerank(msg);
            ragContext = ragService.buildEnhancedSystemPrompt(msg, contextParts);
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
        result.put("intent", intent != null ? intent.name() : "DISABLED");
        result.put("ragEnabled", doRag);
        result.put("memoryEnabled", needMemory);
        result.put("intentEnabled", Boolean.TRUE.equals(needIntent));
        return result;
    }

    /**
     * 解析 RAG 开关：needRag 显式参数优先，否则由意图自动决定
     *
     * @param needRag 用户显式传入的 needRag 参数（可能为 null）
     * @param intent  识别出的用户意图
     * @return 是否启用 RAG
     */
    private boolean resolveRagFlag(Boolean needRag, UserIntent intent) {
        // 显式指定优先
        if (Boolean.TRUE.equals(needRag)) return true;
        if (Boolean.FALSE.equals(needRag)) return false;
        // 未指定时，由意图决定：只有 KNOWLEDGE_QA 触发 RAG
        return intent == UserIntent.KNOWLEDGE_QA;
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
