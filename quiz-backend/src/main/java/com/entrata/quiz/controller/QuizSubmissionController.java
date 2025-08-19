package com.entrata.quiz.controller;

import com.entrata.quiz.dto.QuizResultResponse;
import com.entrata.quiz.dto.QuizSubmissionRequest;
import com.entrata.quiz.service.QuizSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz-submissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quiz Submissions", description = "APIs for submitting quizzes and retrieving results")
public class QuizSubmissionController {
    
    private final QuizSubmissionService quizSubmissionService;
    
    @PostMapping("/submit")
    @Operation(summary = "Submit quiz answers", description = "Submit quiz answers and get immediate scoring and feedback")
    public ResponseEntity<Map<String, Object>> submitQuiz(@Valid @RequestBody QuizSubmissionRequest request) {
        log.info("Received quiz submission for quiz ID: {} by user: {}", 
                request.getQuizId(), request.getUserName());
        
        // Get the quiz attempt
        var attempt = quizSubmissionService.submitQuizAndReturnAttempt(request);
        
        // Return frontend-compatible format
        Map<String, Object> frontendResponse = quizSubmissionService.buildFrontendResponse(attempt, attempt.getQuiz());
        
        return ResponseEntity.ok(frontendResponse);
    }
    
    @GetMapping("/user/{userName}/history")
    @Operation(summary = "Get user quiz history", description = "Retrieve all quiz attempts for a specific user")
    public ResponseEntity<List<QuizResultResponse>> getUserQuizHistory(@PathVariable String userName) {
        log.info("Fetching quiz history for user: {}", userName);
        
        List<QuizResultResponse> history = quizSubmissionService.getUserQuizHistory(userName);
        
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/quiz/{quizId}/attempts")
    @Operation(summary = "Get quiz attempts", description = "Retrieve all attempts for a specific quiz")
    public ResponseEntity<List<QuizResultResponse>> getQuizAttempts(@PathVariable Long quizId) {
        log.info("Fetching all attempts for quiz ID: {}", quizId);
        
        List<QuizResultResponse> attempts = quizSubmissionService.getQuizAttempts(quizId);
        
        return ResponseEntity.ok(attempts);
    }
}
