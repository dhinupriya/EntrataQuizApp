package com.entrata.quiz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Data
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAiConfig {
    
    private String apiKey;
    private String model;
    private String baseUrl;
    
    @PostConstruct
    public void validateConfiguration() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API key is not configured. Please set OPENAI_API_KEY environment variable.");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI model is not configured.");
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI base URL is not configured.");
        }
    }
}
