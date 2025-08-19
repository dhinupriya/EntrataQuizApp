package com.entrata.quiz.controller;

import com.entrata.quiz.config.OpenAiConfig;
import com.entrata.quiz.dto.QuizGenerationRequest;
import com.entrata.quiz.dto.QuizResponse;
import com.entrata.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quiz Management", description = "APIs for generating and managing quizzes")
public class QuizController {
    
    private final QuizService quizService;
    private final OpenAiConfig openAiConfig;
    
    @PostMapping("/generate")
    @Operation(summary = "Generate a new quiz", description = "Generate a quiz with 5 MCQs using AI for a given topic")
    public ResponseEntity<QuizResponse> generateQuiz(@Valid @RequestBody QuizGenerationRequest request) {
        log.info("Received quiz generation request for topic: {}", request.getTopic());
        
        QuizResponse quiz = quizService.generateAndSaveQuiz(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(quiz);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get quiz by ID", description = "Retrieve a specific quiz with all its questions")
    public ResponseEntity<QuizResponse> getQuizById(@PathVariable Long id) {
        log.info("Fetching quiz with ID: {}", id);
        
        QuizResponse quiz = quizService.getQuizById(id);
        
        return ResponseEntity.ok(quiz);
    }
    
    @GetMapping
    @Operation(summary = "Get all quizzes", description = "Retrieve all available quizzes")
    public ResponseEntity<List<QuizResponse>> getAllQuizzes() {
        log.info("Fetching all quizzes");
        
        List<QuizResponse> quizzes = quizService.getAllQuizzes();
        
        return ResponseEntity.ok(quizzes);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search quizzes by topic", description = "Search quizzes containing the specified topic")
    public ResponseEntity<List<QuizResponse>> searchQuizzesByTopic(@RequestParam String topic) {
        log.info("Searching quizzes by topic: {}", topic);
        
        List<QuizResponse> quizzes = quizService.searchQuizzesByTopic(topic);
        
        return ResponseEntity.ok(quizzes);
    }
    
    @GetMapping("/config/check")
    @Operation(summary = "Check OpenAI configuration", description = "Check if OpenAI configuration is properly loaded")
    public ResponseEntity<Map<String, Object>> checkConfiguration() {
        log.info("Checking OpenAI configuration");
        
        Map<String, Object> config = new HashMap<>();
        config.put("status", "Configuration loaded");
        config.put("timestamp", LocalDateTime.now());
        config.put("model", openAiConfig.getModel());
        config.put("baseUrl", openAiConfig.getBaseUrl());
        config.put("apiKeyConfigured", openAiConfig.getApiKey() != null && !openAiConfig.getApiKey().trim().isEmpty());
        config.put("apiKeyPrefix", openAiConfig.getApiKey() != null ? openAiConfig.getApiKey().substring(0, Math.min(10, openAiConfig.getApiKey().length())) : "null");
        
        return ResponseEntity.ok(config);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete quiz", description = "Delete a quiz and all its associated data")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        log.info("Deleting quiz with ID: {}", id);
        
        quizService.deleteQuiz(id);
        
        return ResponseEntity.noContent().build();
    }
}
