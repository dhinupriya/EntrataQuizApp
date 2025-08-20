package com.entrata.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikipediaService {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    private static final String WIKIPEDIA_API_BASE = "https://en.wikipedia.org/api/rest_v1";
    private static final String WIKIPEDIA_SEARCH_API = "https://en.wikipedia.org/w/api.php";
    private static final int MAX_SEARCH_RESULTS = 3;
    private static final int MAX_CONTENT_LENGTH = 2000; // Limit content for OpenAI context
    
    /**
     * Search for Wikipedia articles related to the topic
     */
    public List<WikipediaArticle> searchArticles(String topic) {
        try {
            log.info("Searching Wikipedia for topic: {}", topic);
            
            // Try multiple search variations for better results
            List<String> searchTerms = generateSearchTerms(topic);
            
            for (String searchTerm : searchTerms) {
                String searchUrl = WIKIPEDIA_SEARCH_API + 
                    "?action=query&format=json&list=search&srsearch=" + 
                    searchTerm.replace(" ", "%20") + 
                    "&srlimit=" + MAX_SEARCH_RESULTS;
                
                String response = webClient.get()
                    .uri(searchUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                List<WikipediaArticle> results = parseSearchResults(response);
                if (!results.isEmpty()) {
                    log.info("Found {} articles with search term: {}", results.size(), searchTerm);
                    return results;
                }
            }
            
            log.warn("No Wikipedia articles found for any search variation of: {}", topic);
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("Error searching Wikipedia for topic: {}", topic, e);
            return new ArrayList<>();
        }
    }
    
    private List<String> generateSearchTerms(String topic) {
        List<String> searchTerms = new ArrayList<>();
        String lowerTopic = topic.toLowerCase();
        
        // Original topic
        searchTerms.add(topic);
        
        // Handle specific topic variations
        if (lowerTopic.contains("neural network")) {
            searchTerms.add("neural network");
            searchTerms.add("artificial neural network");
            searchTerms.add("deep learning");
            searchTerms.add("machine learning");
        }
        
        if (lowerTopic.contains("ancient rome") || lowerTopic.contains("rome")) {
            searchTerms.add("ancient rome");
            searchTerms.add("roman empire");
            searchTerms.add("roman history");
        }
        
        if (lowerTopic.contains("photosynthesis")) {
            searchTerms.add("photosynthesis");
            searchTerms.add("plant biology");
        }
        
        // Generic fallbacks
        if (searchTerms.size() == 1) {
            // Try singular/plural variations
            if (topic.endsWith("s")) {
                searchTerms.add(topic.substring(0, topic.length() - 1));
            } else {
                searchTerms.add(topic + "s");
            }
            
            // Try lowercase
            if (!topic.equals(lowerTopic)) {
                searchTerms.add(lowerTopic);
            }
        }
        
        return searchTerms;
    }
    
    /**
     * Get the content of a Wikipedia article
     */
    public WikipediaArticle getArticleContent(String title) {
        try {
            log.info("Fetching Wikipedia content for: {}", title);
            
            String contentUrl = WIKIPEDIA_API_BASE + "/page/summary/" + 
                title.replace(" ", "_");
            
            String response = webClient.get()
                .uri(contentUrl)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> Mono.empty())
                .bodyToMono(String.class)
                .block();
            
            if (response != null) {
                return parseArticleContent(response, title);
            }
            
        } catch (Exception e) {
            log.warn("Could not fetch Wikipedia content for: {}", title, e);
        }
        
        return new WikipediaArticle(title, "", "");
    }
    
    /**
     * Get comprehensive information about a topic
     */
    public List<WikipediaArticle> getTopicInformation(String topic) {
        List<WikipediaArticle> articles = new ArrayList<>();
        
        // First, search for articles
        List<WikipediaArticle> searchResults = searchArticles(topic);
        
        // Then fetch content for each article
        for (WikipediaArticle searchResult : searchResults) {
            WikipediaArticle fullArticle = getArticleContent(searchResult.getTitle());
            if (!fullArticle.getContent().isEmpty()) {
                articles.add(fullArticle);
            }
        }
        
        log.info("Retrieved {} Wikipedia articles for topic: {}", articles.size(), topic);
        return articles;
    }
    
    private List<WikipediaArticle> parseSearchResults(String jsonResponse) {
        List<WikipediaArticle> articles = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode searchResults = root.path("query").path("search");
            
            for (JsonNode result : searchResults) {
                String title = result.path("title").asText();
                String snippet = result.path("snippet").asText();
                
                // Clean HTML tags from snippet
                String cleanSnippet = Jsoup.parse(snippet).text();
                
                articles.add(new WikipediaArticle(title, cleanSnippet, ""));
            }
            
        } catch (Exception e) {
            log.error("Error parsing Wikipedia search results", e);
        }
        
        return articles;
    }
    
    private WikipediaArticle parseArticleContent(String jsonResponse, String title) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            String extract = root.path("extract").asText();
            String url = root.path("content_urls").path("desktop").path("page").asText();
            
            // Limit content length for OpenAI context
            if (extract.length() > MAX_CONTENT_LENGTH) {
                extract = extract.substring(0, MAX_CONTENT_LENGTH) + "...";
            }
            
            return new WikipediaArticle(title, extract, url);
            
        } catch (Exception e) {
            log.error("Error parsing Wikipedia article content for: {}", title, e);
            return new WikipediaArticle(title, "", "");
        }
    }
    
    @Data
    public static class WikipediaArticle {
        private final String title;
        private final String content;
        private final String url;
        
        public WikipediaArticle(String title, String content, String url) {
            this.title = title;
            this.content = content;
            this.url = url;
        }
        
        public boolean hasContent() {
            return content != null && !content.trim().isEmpty();
        }
    }
}
