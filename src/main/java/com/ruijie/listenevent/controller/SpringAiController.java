package com.ruijie.listenevent.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@RestController
public class SpringAiController {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private MessageWindowChatMemory chatMemory;

    /**
     * 同步多轮对话接口：
     * POST http://localhost:9999/ai/chat
     * Body: {"msg": "你好", "conversationId": "可选，不传则自动生成"}
     */
    @PostMapping("/ai/chat")
    public JSONObject chat(@RequestBody JSONObject req) {
        String msg = req.getString("msg");
        String conversationId = req.getString("conversationId");
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        String reply = chatClient.prompt()
                .user(msg)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(conversationId)
                        .build())
                .call()
                .content();

        JSONObject result = new JSONObject();
        result.put("reply", reply);
        result.put("conversationId", conversationId);
        return result;
    }

    /**
     * 流式多轮对话接口（SSE）：
     * POST http://localhost:9999/ai/stream-chat
     * Body: {"msg": "你好", "conversationId": "可选，不传则自动生成"}
     */
    @PostMapping("/ai/stream-chat")
    public SseEmitter streamChat(@RequestBody JSONObject req) {
        String msg = req.getString("msg");
        String conversationId = req.getString("conversationId");
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        SseEmitter emitter = new SseEmitter(60_000L);
        final String cid = conversationId;

        new Thread(() -> {
            try {
                chatClient.prompt()
                        .user(msg)
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                                .conversationId(cid)
                                .build())
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
}
