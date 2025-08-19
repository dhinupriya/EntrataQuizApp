package com.entrata.quiz.service;

import com.entrata.quiz.dto.QuizGenerationRequest;
import com.entrata.quiz.dto.QuizResponse;
import com.entrata.quiz.entity.Quiz;
import com.entrata.quiz.repository.QuizRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private OpenAiService openAiService;

    @InjectMocks
    private QuizService quizService;

    private Quiz sampleQuiz;
    private QuizGenerationRequest sampleRequest;

    @BeforeEach
    void setUp() {
        // Create sample quiz
        sampleQuiz = Quiz.builder()
                .id(1L)
                .topic("Java")
                .title("Java Basics Quiz")
                .description("Test your knowledge of Java fundamentals")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .questions(new ArrayList<>())
                .build();

        // Create sample request
        sampleRequest = new QuizGenerationRequest();
        sampleRequest.setTopic("Java");
    }

    @Test
    void getAllQuizzes_ShouldReturnListOfQuizzes() {
        // Given
        List<Quiz> quizzes = List.of(sampleQuiz);
        when(quizRepository.findAllByOrderByCreatedAtDesc()).thenReturn(quizzes);

        // When
        List<QuizResponse> result = quizService.getAllQuizzes();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).getTopic());
        assertEquals("Java Basics Quiz", result.get(0).getTitle());
        
        verify(quizRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getAllQuizzes_ShouldReturnEmptyListWhenNoQuizzes() {
        // Given
        when(quizRepository.findAllByOrderByCreatedAtDesc()).thenReturn(new ArrayList<>());

        // When
        List<QuizResponse> result = quizService.getAllQuizzes();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(quizRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getQuizById_ShouldReturnQuizWhenExists() {
        // Given
        Long quizId = 1L;
        when(quizRepository.findById(quizId)).thenReturn(Optional.of(sampleQuiz));

        // When
        QuizResponse result = quizService.getQuizById(quizId);

        // Then
        assertNotNull(result);
        assertEquals("Java", result.getTopic());
        assertEquals("Java Basics Quiz", result.getTitle());
        
        verify(quizRepository, times(1)).findById(quizId);
    }

    @Test
    void getQuizById_ShouldThrowExceptionWhenNotExists() {
        // Given
        Long quizId = 999L;
        when(quizRepository.findById(quizId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quizService.getQuizById(quizId);
        });
        
        assertEquals("Quiz not found with ID: 999", exception.getMessage());
        verify(quizRepository, times(1)).findById(quizId);
    }

    @Test
    void generateAndSaveQuiz_ShouldGenerateAndSaveQuizSuccessfully() {
        // Given
        when(openAiService.generateQuiz(any(QuizGenerationRequest.class))).thenReturn(sampleQuiz);
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);

        // When
        QuizResponse result = quizService.generateAndSaveQuiz(sampleRequest);

        // Then
        assertNotNull(result);
        assertEquals("Java", result.getTopic());
        assertEquals("Java Basics Quiz", result.getTitle());
        
        verify(openAiService, times(1)).generateQuiz(sampleRequest);
        verify(quizRepository, times(1)).save(any(Quiz.class));
    }

    @Test
    void generateAndSaveQuiz_ShouldHandleOpenAiServiceException() {
        // Given
        when(openAiService.generateQuiz(any(QuizGenerationRequest.class)))
                .thenThrow(new RuntimeException("OpenAI service error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quizService.generateAndSaveQuiz(sampleRequest);
        });
        
        assertEquals("OpenAI service error", exception.getMessage());
        verify(openAiService, times(1)).generateQuiz(sampleRequest);
        verify(quizRepository, never()).save(any(Quiz.class));
    }

    @Test
    void generateAndSaveQuiz_ShouldHandleRepositoryException() {
        // Given
        when(openAiService.generateQuiz(any(QuizGenerationRequest.class))).thenReturn(sampleQuiz);
        when(quizRepository.save(any(Quiz.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quizService.generateAndSaveQuiz(sampleRequest);
        });
        
        assertEquals("Database error", exception.getMessage());
        verify(openAiService, times(1)).generateQuiz(sampleRequest);
        verify(quizRepository, times(1)).save(any(Quiz.class));
    }
}