package com.entrata.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StackOverflowService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    private static final String STACKOVERFLOW_API_BASE = "https://api.stackexchange.com/2.3";
    private static final int MAX_SEARCH_RESULTS = 3;
    private static final int MAX_CONTENT_LENGTH = 2000;
    
    /**
     * Search for Stack Overflow questions and answers related to the topic
     */
    public List<StackOverflowAnswer> searchAnswers(String topic) {
        try {
            log.info("Searching Stack Overflow for topic: {}", topic);
            
            // Search for questions with high scores and accepted answers
            String searchUrl = STACKOVERFLOW_API_BASE + "/search/advanced" +
                "?order=desc&sort=votes&accepted=True&answers=1" +
                "&q=" + topic.replace(" ", "%20") +
                "&site=stackoverflow" +
                "&pagesize=" + MAX_SEARCH_RESULTS +
                "&filter=withbody";
            
            String response = webClient.get()
                .uri(searchUrl)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> Mono.empty())
                .bodyToMono(String.class)
                .block();
            
            if (response != null) {
                return parseStackOverflowResponse(response);
            }
            
            log.warn("No Stack Overflow results found for topic: {}", topic);
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Error searching Stack Overflow for topic: {}", topic, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get comprehensive information about a topic from Stack Overflow
     */
    public List<StackOverflowAnswer> getTopicInformation(String topic) {
        List<StackOverflowAnswer> answers = new ArrayList<>();
        
        // Search for high-quality answers
        List<StackOverflowAnswer> searchResults = searchAnswers(topic);
        
        for (StackOverflowAnswer answer : searchResults) {
            if (answer.hasContent()) {
                answers.add(answer);
            }
        }
        
        log.info("Retrieved {} Stack Overflow answers for topic: {}", answers.size(), topic);
        return answers;
    }
    
    private List<StackOverflowAnswer> parseStackOverflowResponse(String jsonResponse) {
        List<StackOverflowAnswer> answers = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("items");
            
            for (JsonNode item : items) {
                String title = item.path("title").asText();
                String body = item.path("body").asText();
                int score = item.path("score").asInt();
                String link = item.path("link").asText();
                
                // Clean HTML tags from body
                String cleanBody = Jsoup.parse(body).text();
                
                // Limit content length
                if (cleanBody.length() > MAX_CONTENT_LENGTH) {
                    cleanBody = cleanBody.substring(0, MAX_CONTENT_LENGTH) + "...";
                }
                
                answers.add(new StackOverflowAnswer(title, cleanBody, score, link));
            }
            
        } catch (Exception e) {
            log.error("Error parsing Stack Overflow response", e);
        }
        
        return answers;
    }
    
    @Data
    public static class StackOverflowAnswer {
        private final String title;
        private final String content;
        private final int score;
        private final String url;
        
        public StackOverflowAnswer(String title, String content, int score, String url) {
            this.title = title;
            this.content = content;
            this.score = score;
            this.url = url;
        }
        
        public boolean hasContent() {
            return content != null && !content.trim().isEmpty() && score > 0;
        }
    }
}
