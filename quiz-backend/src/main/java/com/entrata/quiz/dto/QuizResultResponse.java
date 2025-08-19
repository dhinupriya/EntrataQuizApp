package com.entrata.quiz.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizResultResponse {
    
    private Long quizId;
    private String quizTitle;
    private String userName;
    private Integer score;
    private Integer totalQuestions;
    private Double percentage;
    private LocalDateTime submittedAt;
    private List<QuestionResult> questionResults;
    
    @Data
    @Builder
    public static class QuestionResult {
        private Long questionId;
        private String questionText;
        private String selectedAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private String explanation;
        private String feedback;
    }
}
