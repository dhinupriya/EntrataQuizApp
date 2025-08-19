package com.entrata.quiz.service;

import com.entrata.quiz.dto.QuizResultResponse;
import com.entrata.quiz.dto.QuizSubmissionRequest;
import com.entrata.quiz.entity.*;
import com.entrata.quiz.repository.QuizAttemptRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizSubmissionServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @InjectMocks
    private QuizSubmissionService quizSubmissionService;

    private Quiz sampleQuiz;
    private Question sampleQuestion;
    private QuestionOption sampleOptionA;
    private QuestionOption sampleOptionB;
    private QuizSubmissionRequest sampleRequest;
    private QuizAttempt sampleAttempt;

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

        // Create sample options
        sampleOptionA = QuestionOption.builder()
                .id(1L)
                .optionLabel("A")
                .optionText("true")
                .build();

        sampleOptionB = QuestionOption.builder()
                .id(2L)
                .optionLabel("B")
                .optionText("false")
                .build();

        // Create sample question
        sampleQuestion = Question.builder()
                .id(1L)
                .questionText("What is the default value of a boolean variable in Java?")
                .correctAnswer("false")
                .explanation("The default value of a boolean variable in Java is false.")
                .questionNumber(1)
                .quiz(sampleQuiz)
                .options(List.of(sampleOptionA, sampleOptionB))
                .build();

        sampleQuiz.getQuestions().add(sampleQuestion);

        // Create sample request
        QuizSubmissionRequest.QuestionAnswer questionAnswer = new QuizSubmissionRequest.QuestionAnswer();
        questionAnswer.setQuestionId(1L);
        questionAnswer.setSelectedAnswer("1"); // Index for "false"
        
        sampleRequest = new QuizSubmissionRequest();
        sampleRequest.setQuizId(1L);
        sampleRequest.setUserName("Test User");
        sampleRequest.setAnswers(List.of(questionAnswer));

        // Create sample attempt
        sampleAttempt = QuizAttempt.builder()
                .id(1L)
                .quiz(sampleQuiz)
                .userName("Test User")
                .score(1)
                .totalQuestions(1)
                .submittedAt(LocalDateTime.now())
                .responses(new ArrayList<>())
                .build();
    }

    @Test
    void submitQuiz_ShouldReturnSuccessfulResult() {
        // Given
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(sampleAttempt);

        // When
        QuizResultResponse result = quizSubmissionService.submitQuiz(sampleRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getScore());
        assertEquals(1, result.getTotalQuestions());
        assertEquals("Test User", result.getUserName());
        assertNotNull(result.getQuestionResults());
        
        verify(quizRepository, times(1)).findById(1L);
        verify(quizAttemptRepository, times(1)).save(any(QuizAttempt.class));
    }

    @Test
    void submitQuiz_ShouldHandleQuizNotFound() {
        // Given
        when(quizRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            quizSubmissionService.submitQuiz(sampleRequest);
        });
        
        assertEquals("Quiz not found with ID: 1", exception.getMessage());
        verify(quizRepository, times(1)).findById(1L);
        verify(quizAttemptRepository, never()).save(any(QuizAttempt.class));
    }

    @Test
    void submitQuizAndReturnAttempt_ShouldCalculateCorrectScore() {
        // Given
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(quizAttemptRepository.save(any(QuizAttempt.class))).thenReturn(sampleAttempt);

        // When
        QuizAttempt result = quizSubmissionService.submitQuizAndReturnAttempt(sampleRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getScore());
        assertEquals(1, result.getTotalQuestions());
        assertEquals("Test User", result.getUserName());
        
        verify(quizRepository, times(1)).findById(1L);
        verify(quizAttemptRepository, times(1)).save(any(QuizAttempt.class));
    }

    @Test
    void buildFrontendResponse_ShouldCreateCorrectResponse() {
        // Given - Create a proper quiz attempt with responses
        QuestionResponse questionResponse = QuestionResponse.builder()
                .question(sampleQuestion)
                .selectedAnswer("false")
                .isCorrect(true)
                .feedback("Correct! The default value of a boolean variable in Java is false.")
                .build();
        
        sampleAttempt.setResponses(List.of(questionResponse));

        // When
        Map<String, Object> result = quizSubmissionService.buildFrontendResponse(sampleAttempt, sampleQuiz);

        // Then
        assertNotNull(result);
        assertEquals(1, result.get("score"));
        assertEquals(1, result.get("totalQuestions"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> feedback = (List<Map<String, Object>>) result.get("feedback");
        assertNotNull(feedback);
        assertEquals(1, feedback.size());
        
        Map<String, Object> feedbackItem = feedback.get(0);
        assertEquals(0, feedbackItem.get("questionIndex")); // 0-based index
        assertEquals(true, feedbackItem.get("correct"));
        assertNotNull(feedbackItem.get("explanation"));
        assertEquals("false", feedbackItem.get("correctAnswer"));
    }

    @Test
    void getUserQuizHistory_ShouldReturnUserAttempts() {
        // Given
        String userName = "Test User";
        when(quizAttemptRepository.findByUserNameOrderBySubmittedAtDesc(userName))
                .thenReturn(List.of(sampleAttempt));

        // When
        List<QuizResultResponse> result = quizSubmissionService.getUserQuizHistory(userName);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test User", result.get(0).getUserName());
        
        verify(quizAttemptRepository, times(1)).findByUserNameOrderBySubmittedAtDesc(userName);
    }

    @Test
    void getQuizAttempts_ShouldReturnQuizAttempts() {
        // Given
        Long quizId = 1L;
        when(quizAttemptRepository.findByQuizIdOrderBySubmittedAtDesc(quizId))
                .thenReturn(List.of(sampleAttempt));

        // When
        List<QuizResultResponse> result = quizSubmissionService.getQuizAttempts(quizId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getQuizId());
        
        verify(quizAttemptRepository, times(1)).findByQuizIdOrderBySubmittedAtDesc(quizId);
    }
}