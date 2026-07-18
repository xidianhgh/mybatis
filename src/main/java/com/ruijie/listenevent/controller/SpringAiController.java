package com.ruijie.listenevent.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
public class SpringAiController {

    @Autowired
    private OllamaChatModel ollamaChatModel;

    /**
     * 同步对话接口：http://localhost:9999/ai/chat?msg=你的问题
     */
    @PostMapping("/ai/chat")
    public String chat(@RequestBody JSONObject req) {
        String msg = req.getString("msg");
        ChatResponse response = ollamaChatModel.call(new Prompt(msg));
        return response.getResult().getOutput().getText();
    }

    /**
     * 流式对话接口（SSE）：http://localhost:9999/ai/stream-chat?msg=你的问题
     */
    @PostMapping("/ai/stream-chat")
    public SseEmitter streamChat(@RequestBody JSONObject req) {
        String msg = req.getString("msg");
        SseEmitter emitter = new SseEmitter(60_000L);

        // 在异步线程中流式调用模型
        new Thread(() -> {
            try {
                ollamaChatModel.stream(new Prompt(msg))
                        .doOnNext(response -> {
                            String text = response.getResult().getOutput().getText();
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
