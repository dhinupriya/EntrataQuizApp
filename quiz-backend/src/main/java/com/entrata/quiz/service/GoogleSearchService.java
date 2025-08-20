package com.entrata.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleSearchService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${google.search.api-key:}")
    private String apiKey;
    
    @Value("${google.search.cx:}")
    private String customSearchEngineId;
    
    private static final String GOOGLE_SEARCH_API_BASE = "https://www.googleapis.com/customsearch/v1";
    private static final int MAX_SEARCH_RESULTS = 3;
    private static final int MAX_CONTENT_LENGTH = 2000;
    
    /**
     * Search for educational content using Google Custom Search
     */
    public List<GoogleSearchResult> searchEducationalContent(String topic) {
        try {
            if (apiKey.isEmpty() || customSearchEngineId.isEmpty()) {
                log.warn("Google Search API not configured, skipping Google search");
                return new ArrayList<>();
            }
            
            log.info("Searching Google for educational content on topic: {}", topic);
            
            // Search for educational content with site restrictions
            String searchUrl = GOOGLE_SEARCH_API_BASE +
                "?key=" + apiKey +
                "&cx=" + customSearchEngineId +
                "&q=" + topic.replace(" ", "%20") + " tutorial explanation" +
                "&num=" + MAX_SEARCH_RESULTS +
                "&siteSearch=edu OR site:stackoverflow.com OR site:github.com";
            
            String response = webClient.get()
                .uri(searchUrl)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> Mono.empty())
                .bodyToMono(String.class)
                .block();
            
            if (response != null) {
                return parseGoogleSearchResponse(response);
            }
            
            log.warn("No Google search results found for topic: {}", topic);
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Error searching Google for topic: {}", topic, e);
            return new ArrayList<>();
        }
    }
    
    private List<GoogleSearchResult> parseGoogleSearchResponse(String jsonResponse) {
        List<GoogleSearchResult> results = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode items = root.path("items");
            
            for (JsonNode item : items) {
                String title = item.path("title").asText();
                String snippet = item.path("snippet").asText();
                String link = item.path("link").asText();
                
                // Clean and limit content length
                String cleanSnippet = Jsoup.parse(snippet).text();
                if (cleanSnippet.length() > MAX_CONTENT_LENGTH) {
                    cleanSnippet = cleanSnippet.substring(0, MAX_CONTENT_LENGTH) + "...";
                }
                
                results.add(new GoogleSearchResult(title, cleanSnippet, link));
            }
            
        } catch (Exception e) {
            log.error("Error parsing Google search response", e);
        }
        
        return results;
    }
    
    @Data
    public static class GoogleSearchResult {
        private final String title;
        private final String content;
        private final String url;
        
        public GoogleSearchResult(String title, String content, String url) {
            this.title = title;
            this.content = content;
            this.url = url;
        }
        
        public boolean hasContent() {
            return content != null && !content.trim().isEmpty();
        }
    }
}
