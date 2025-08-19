package com.entrata.quiz.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quiz_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
    
    @Column(nullable = false)
    private String userName;
    
    @Column(nullable = false)
    private Integer score;
    
    @Column(nullable = false)
    private Integer totalQuestions;
    
    @Column(nullable = false)
    private LocalDateTime submittedAt;
    
    @OneToMany(mappedBy = "quizAttempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuestionResponse> responses;
    
    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
