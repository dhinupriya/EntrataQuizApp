package com.entrata.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuizGenerationRequest {
    
    @NotBlank(message = "Topic is required")
    @Size(min = 3, max = 100, message = "Topic must be between 3 and 100 characters")
    private String topic;
    
    @Size(max = 200, message = "Description must not exceed 200 characters")
    private String description;
}
