package com.entrata.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuizSubmissionRequest {
    
    @NotNull(message = "Quiz ID is required")
    private Long quizId;
    
    @NotBlank(message = "User name is required")
    private String userName;
    
    @NotEmpty(message = "Answers are required")
    private List<QuestionAnswer> answers;
    
    @Data
    public static class QuestionAnswer {
        @NotNull(message = "Question ID is required")
        private Long questionId;
        
        @NotBlank(message = "Selected answer is required")
        private String selectedAnswer;
    }
}
