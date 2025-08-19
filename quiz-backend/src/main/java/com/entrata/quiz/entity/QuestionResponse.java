package com.entrata.quiz.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "question_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_attempt_id", nullable = false)
    private QuizAttempt quizAttempt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
    
    @Column(nullable = false)
    private String selectedAnswer;
    
    @Column(nullable = false)
    private Boolean isCorrect;
    
    @Column(columnDefinition = "TEXT")
    private String feedback;
}
