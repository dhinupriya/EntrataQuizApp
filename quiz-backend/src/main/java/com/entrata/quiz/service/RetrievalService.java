package com.entrata.quiz.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetrievalService {
    
    private final WikipediaService wikipediaService;
    private final StackOverflowService stackOverflowService;
    private final GoogleSearchService googleSearchService;
    
    /**
     * Retrieve relevant context for a given topic to improve quiz accuracy
     */
    public RetrievalContext retrieveContext(String topic) {
        log.info("Retrieving context for topic: {}", topic);
        
        try {
            List<Source> allSources = new ArrayList<>();
            StringBuilder combinedContent = new StringBuilder();
            
            // Try Wikipedia first
            List<WikipediaService.WikipediaArticle> wikipediaArticles = 
                wikipediaService.getTopicInformation(topic);
            
            if (!wikipediaArticles.isEmpty()) {
                String wikipediaContent = combineWikipediaContent(wikipediaArticles);
                if (!wikipediaContent.trim().isEmpty()) {
                    combinedContent.append("WIKIPEDIA SOURCES:\n").append(wikipediaContent).append("\n\n");
                    
                    allSources.addAll(wikipediaArticles.stream()
                        .filter(WikipediaService.WikipediaArticle::hasContent)
                        .map(article -> new Source(article.getTitle(), article.getUrl(), "Wikipedia"))
                        .collect(Collectors.toList()));
                }
            }
            
            // Try Stack Overflow for technical topics
            if (isTechnicalTopic(topic)) {
                List<StackOverflowService.StackOverflowAnswer> stackOverflowAnswers = 
                    stackOverflowService.getTopicInformation(topic);
                
                if (!stackOverflowAnswers.isEmpty()) {
                    String stackOverflowContent = combineStackOverflowContent(stackOverflowAnswers);
                    if (!stackOverflowContent.trim().isEmpty()) {
                        combinedContent.append("STACK OVERFLOW SOURCES:\n").append(stackOverflowContent).append("\n\n");
                        
                        allSources.addAll(stackOverflowAnswers.stream()
                            .filter(StackOverflowService.StackOverflowAnswer::hasContent)
                            .map(answer -> new Source(answer.getTitle(), answer.getUrl(), "Stack Overflow"))
                            .collect(Collectors.toList()));
                    }
                }
            }
            
            // Try Google Search as fallback
            if (allSources.isEmpty()) {
                List<GoogleSearchService.GoogleSearchResult> googleResults = 
                    googleSearchService.searchEducationalContent(topic);
                
                if (!googleResults.isEmpty()) {
                    String googleContent = combineGoogleContent(googleResults);
                    if (!googleContent.trim().isEmpty()) {
                        combinedContent.append("EDUCATIONAL SOURCES:\n").append(googleContent).append("\n\n");
                        
                        allSources.addAll(googleResults.stream()
                            .filter(GoogleSearchService.GoogleSearchResult::hasContent)
                            .map(result -> new Source(result.getTitle(), result.getUrl(), "Educational"))
                            .collect(Collectors.toList()));
                    }
                }
            }
            
            if (allSources.isEmpty()) {
                log.warn("No sources found for topic: {}", topic);
                return new RetrievalContext(topic, "", new ArrayList<>());
            }
            
            log.info("Retrieved context from {} sources for topic: {}", allSources.size(), topic);
            
            return new RetrievalContext(topic, combinedContent.toString().trim(), allSources);
            
        } catch (Exception e) {
            log.error("Error retrieving context for topic: {}", topic, e);
            return new RetrievalContext(topic, "", new ArrayList<>());
        }
    }
    
    /**
     * Check if retrieval is beneficial for the given topic
     */
    public boolean shouldUseRetrieval(String topic) {
        // Use retrieval for topics that are likely to benefit from factual information
        String lowerTopic = topic.toLowerCase();
        
        // Topics that benefit from factual retrieval
        String[] factualTopics = {
            "history", "science", "biology", "chemistry", "physics", "geography",
            "mathematics", "literature", "philosophy", "economics", "politics",
            "medicine", "technology", "computer", "engineering", "astronomy",
            "geology", "psychology", "sociology", "anthropology", "archaeology",
            "photosynthesis", "evolution", "genetics", "anatomy", "ecology",
            "molecular", "cellular", "biochemistry", "organic", "inorganic",
            "neural", "network", "machine learning", "artificial intelligence",
            "rome", "roman", "ancient", "empire", "civilization"
        };
        
        // Programming topics that might benefit from current information
        String[] programmingTopics = {
            "java", "python", "javascript", "react", "spring", "node", "angular",
            "vue", "docker", "kubernetes", "aws", "azure", "database", "sql"
        };
        
        for (String factualTopic : factualTopics) {
            if (lowerTopic.contains(factualTopic)) {
                return true;
            }
        }
        
        for (String progTopic : programmingTopics) {
            if (lowerTopic.contains(progTopic)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isTechnicalTopic(String topic) {
        String lowerTopic = topic.toLowerCase();
        String[] technicalKeywords = {
            "programming", "coding", "software", "algorithm", "data structure",
            "neural", "machine learning", "ai", "artificial intelligence",
            "java", "python", "javascript", "react", "spring", "node",
            "database", "sql", "api", "framework", "library"
        };
        
        for (String keyword : technicalKeywords) {
            if (lowerTopic.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private String combineWikipediaContent(List<WikipediaService.WikipediaArticle> articles) {
        StringBuilder combinedContent = new StringBuilder();
        
        for (int i = 0; i < articles.size() && i < 3; i++) { // Limit to top 3 articles
            WikipediaService.WikipediaArticle article = articles.get(i);
            
            if (article.hasContent()) {
                combinedContent.append("Source: ").append(article.getTitle()).append("\n");
                combinedContent.append(article.getContent()).append("\n\n");
            }
        }
        
        return combinedContent.toString().trim();
    }
    
    private String combineStackOverflowContent(List<StackOverflowService.StackOverflowAnswer> answers) {
        StringBuilder combinedContent = new StringBuilder();
        
        for (int i = 0; i < answers.size() && i < 3; i++) { // Limit to top 3 answers
            StackOverflowService.StackOverflowAnswer answer = answers.get(i);
            
            if (answer.hasContent()) {
                combinedContent.append("Q: ").append(answer.getTitle()).append("\n");
                combinedContent.append("A (Score: ").append(answer.getScore()).append("): ");
                combinedContent.append(answer.getContent()).append("\n\n");
            }
        }
        
        return combinedContent.toString().trim();
    }
    
    private String combineGoogleContent(List<GoogleSearchService.GoogleSearchResult> results) {
        StringBuilder combinedContent = new StringBuilder();
        
        for (int i = 0; i < results.size() && i < 3; i++) { // Limit to top 3 results
            GoogleSearchService.GoogleSearchResult result = results.get(i);
            
            if (result.hasContent()) {
                combinedContent.append("Source: ").append(result.getTitle()).append("\n");
                combinedContent.append(result.getContent()).append("\n\n");
            }
        }
        
        return combinedContent.toString().trim();
    }
    
    @Data
    public static class RetrievalContext {
        private final String topic;
        private final String content;
        private final List<Source> sources;
        
        public boolean hasContent() {
            return content != null && !content.trim().isEmpty();
        }
        
        public String getContextForPrompt() {
            if (!hasContent()) {
                return "";
            }
            
            return "FACTUAL CONTEXT (use this information to ensure accuracy):\n" +
                   content + "\n\n" +
                   "Please use the above factual information to create accurate quiz questions. " +
                   "Ensure all facts, dates, names, and technical details are correct based on the provided context.\n\n";
        }
    }
    
    @Data
    public static class Source {
        private final String title;
        private final String url;
        private final String type;
    }
}
