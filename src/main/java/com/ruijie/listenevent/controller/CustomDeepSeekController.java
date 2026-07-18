package com.ruijie.listenevent.controller;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
public class CustomDeepSeekController {

    /**
     * 对话接口控制器
     */
    @RestController
    static class DeepSeekChatController {
        private static final String OLLAMA_CHAT_API = "http://localhost:11434/api/chat";
        private static final String MODEL_NAME = "mydeep";
        private final WebClient webClient;

        // 注入 WebClient 构建器
        public DeepSeekChatController(WebClient.Builder webClientBuilder) {
            this.webClient = webClientBuilder.build();
        }

        // 同步对话接口：http://localhost:8080/chat?msg=你的问题
        @GetMapping("/chat")
        public String chat(@RequestParam String msg) {
            // 构建请求体
            OllamaChatRequest request = new OllamaChatRequest(
                    MODEL_NAME,
                    List.of(new OllamaMessage("user", msg)),
                    false
            );

            // 同步调用 Ollama API
            OllamaChatResponse response = webClient.post()
                    .uri(OLLAMA_CHAT_API)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaChatResponse.class)
                    .block();

            // 返回回复内容
            return response != null ? response.getMessage().getContent() : "模型无响应";
        }

        // 流式对话接口（逐字返回）：http://localhost:8080/stream-chat?msg=你的问题
        @PostMapping("/stream-chat")
        public Flux<String> streamChat(@RequestBody JSONObject req) {
            String msg = req.getString("msg");
            OllamaChatRequest request = new OllamaChatRequest(
                    MODEL_NAME,
                    List.of(new OllamaMessage("user", msg)),
                    true
            );

            // 流式读取响应，逐行返回
            return webClient.post()
                    .uri(OLLAMA_CHAT_API)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .map(line -> {
                        // 简化解析，提取回复内容
                        if (line.contains("\"content\":\"")) {
                            String content = line.split("\"content\":\"")[1].split("\"")[0];
                            return content.replace("\\n", "\n");
                        }
                        return "";
                    })
                    .filter(content -> !content.isEmpty());
        }
    }

    // ========== 适配 Ollama API 的实体类 ==========

    /**
     * Ollama 聊天请求体
     */
    static class OllamaChatRequest {
        private String model;
        private List<OllamaMessage> messages;
        @JsonProperty("stream")
        private boolean stream;

        public OllamaChatRequest(String model, List<OllamaMessage> messages, boolean stream) {
            this.model = model;
            this.messages = messages;
            this.stream = stream;
        }

        // Getter & Setter（IDEA 自动生成即可）
        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<OllamaMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<OllamaMessage> messages) {
            this.messages = messages;
        }

        public boolean isStream() {
            return stream;
        }

        public void setStream(boolean stream) {
            this.stream = stream;
        }
    }

    /**
     * 消息实体（角色+内容）
     */
    static class OllamaMessage {
        private String role;
        private String content;

        public OllamaMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        // Getter & Setter
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /**
     * Ollama 聊天响应体
     */
    static class OllamaChatResponse {
        private OllamaMessage message;

        // Getter & Setter
        public OllamaMessage getMessage() {
            return message;
        }

        public void setMessage(OllamaMessage message) {
            this.message = message;
        }
    }
}
