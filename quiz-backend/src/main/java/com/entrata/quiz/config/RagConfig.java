package com.entrata.quiz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {
    
    private boolean enabled = true;
    private Wikipedia wikipedia = new Wikipedia();
    private Retrieval retrieval = new Retrieval();
    
    @Data
    public static class Wikipedia {
        private boolean enabled = true;
        private int maxArticles = 3;
        private int maxContentLength = 2000;
    }
    
    @Data
    public static class Retrieval {
        private int timeoutSeconds = 10;
        private boolean fallbackOnError = true;
    }
}
