package com.entrata.quiz.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "question_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String optionLabel; // A, B, C, D
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String optionText;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;
}
