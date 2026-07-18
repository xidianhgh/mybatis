package com.ruijie.listenevent.common.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.model:mydeep}")
    private String model;

    @Value("${spring.ai.ollama.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.ollama.options.num-ctx:2048}")
    private Integer numCtx;

    @Value("${spring.ai.ollama.options.top-p:0.95}")
    private Double topP;

    @Bean
    public OllamaChatModel ollamaChatModel() {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .numCtx(numCtx)
                        .topP(topP)
                        .build())
                .build();
    }
}
