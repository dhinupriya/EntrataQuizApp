package com.entrata.quiz.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizResponse {
    
    private Long id;
    private String topic;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private List<QuestionResponse> questions;
    
    @Data
    @Builder
    public static class QuestionResponse {
        private Long id;
        private String questionText;
        private List<QuestionOptionResponse> options;
        private Integer questionNumber;
        private String correctAnswer; // Add this field
    }
    
    @Data
    @Builder
    public static class QuestionOptionResponse {
        private String optionLabel;
        private String optionText;
    }
}
