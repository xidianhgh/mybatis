package com.ruijie.listenevent.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        private static final ExecutorService executor = Executors.newCachedThreadPool();

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
        @GetMapping(value = "/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter streamChat(@RequestParam String msg) {
            SseEmitter emitter = new SseEmitter(0L); // 不超时

            executor.execute(() -> {
                try {
                    // 构建请求体
                    String requestBody = String.format(
                            "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"stream\":true}",
                            MODEL_NAME, msg.replace("\"", "\\\"")
                    );

                    HttpURLConnection conn = (HttpURLConnection) URI.create(OLLAMA_CHAT_API).toURL().openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(requestBody.getBytes(StandardCharsets.UTF_8));
                    conn.getOutputStream().flush();

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("\"content\":\"")) {
                                String content = line.split("\"content\":\"")[1].split("\"")[0];
                                content = content.replace("\\n", "\n");
                                if (!content.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(content));
                                }
                            }
                        }
                    }
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            });

            return emitter;
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

        public OllamaMessage getMessage() {
            return message;
        }

        public void setMessage(OllamaMessage message) {
            this.message = message;
        }
    }
}
