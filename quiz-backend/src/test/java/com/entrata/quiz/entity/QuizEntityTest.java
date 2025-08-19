package com.entrata.quiz.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuizEntityTest {

    private Quiz quiz;
    private Question question;
    private QuestionOption option;
    private QuizAttempt quizAttempt;
    private QuestionResponse questionResponse;

    @BeforeEach
    void setUp() {
        // Create Quiz
        quiz = Quiz.builder()
                .id(1L)
                .topic("Java")
                .title("Java Basics Quiz")
                .description("Test your knowledge of Java fundamentals")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .questions(new ArrayList<>())
                .build();

        // Create Question
        question = Question.builder()
                .id(1L)
                .questionText("What is the default value of a boolean variable in Java?")
                .correctAnswer("false")
                .explanation("The default value of a boolean variable in Java is false.")
                .questionNumber(1)
                .quiz(quiz)
                .options(new ArrayList<>())
                .build();

        // Create QuestionOption
        option = QuestionOption.builder()
                .id(1L)
                .optionLabel("A")
                .optionText("true")
                .question(question)
                .build();

        // Create QuizAttempt
        quizAttempt = QuizAttempt.builder()
                .id(1L)
                .quiz(quiz)
                .userName("Test User")
                .score(1)
                .totalQuestions(1)
                .submittedAt(LocalDateTime.now())
                .responses(new ArrayList<>())
                .build();

        // Create QuestionResponse
        questionResponse = QuestionResponse.builder()
                .id(1L)
                .question(question)
                .quizAttempt(quizAttempt)
                .selectedAnswer("false")
                .isCorrect(true)
                .feedback("Correct! The default value of a boolean variable in Java is false.")
                .build();
    }

    @Test
    void quiz_ShouldHaveCorrectProperties() {
        // Then
        assertEquals(1L, quiz.getId());
        assertEquals("Java", quiz.getTopic());
        assertEquals("Java Basics Quiz", quiz.getTitle());
        assertEquals("Test your knowledge of Java fundamentals", quiz.getDescription());
        assertNotNull(quiz.getCreatedAt());
        assertNotNull(quiz.getUpdatedAt());
        assertNotNull(quiz.getQuestions());
        assertTrue(quiz.getQuestions().isEmpty());
    }

    @Test
    void quiz_ShouldAllowAddingQuestions() {
        // When
        quiz.getQuestions().add(question);

        // Then
        assertEquals(1, quiz.getQuestions().size());
        assertEquals(question, quiz.getQuestions().get(0));
    }

    @Test
    void quiz_ShouldSupportBuilderPattern() {
        // When
        Quiz newQuiz = Quiz.builder()
                .topic("Python")
                .title("Python Basics")
                .description("Learn Python")
                .build();

        // Then
        assertEquals("Python", newQuiz.getTopic());
        assertEquals("Python Basics", newQuiz.getTitle());
        assertEquals("Learn Python", newQuiz.getDescription());
    }

    @Test
    void question_ShouldHaveCorrectProperties() {
        // Then
        assertEquals(1L, question.getId());
        assertEquals("What is the default value of a boolean variable in Java?", question.getQuestionText());
        assertEquals("false", question.getCorrectAnswer());
        assertEquals("The default value of a boolean variable in Java is false.", question.getExplanation());
        assertEquals(1, question.getQuestionNumber());
        assertEquals(quiz, question.getQuiz());
        assertNotNull(question.getOptions());
        assertTrue(question.getOptions().isEmpty());
    }

    @Test
    void question_ShouldAllowAddingOptions() {
        // When
        question.getOptions().add(option);

        // Then
        assertEquals(1, question.getOptions().size());
        assertEquals(option, question.getOptions().get(0));
    }

    @Test
    void question_ShouldSupportBuilderPattern() {
        // When
        Question newQuestion = Question.builder()
                .questionText("What is Java?")
                .correctAnswer("A programming language")
                .explanation("Java is a programming language")
                .questionNumber(2)
                .build();

        // Then
        assertEquals("What is Java?", newQuestion.getQuestionText());
        assertEquals("A programming language", newQuestion.getCorrectAnswer());
        assertEquals("Java is a programming language", newQuestion.getExplanation());
        assertEquals(2, newQuestion.getQuestionNumber());
    }

    @Test
    void questionOption_ShouldHaveCorrectProperties() {
        // Then
        assertEquals(1L, option.getId());
        assertEquals("A", option.getOptionLabel());
        assertEquals("true", option.getOptionText());
        assertEquals(question, option.getQuestion());
    }

    @Test
    void questionOption_ShouldSupportBuilderPattern() {
        // When
        QuestionOption newOption = QuestionOption.builder()
                .optionLabel("B")
                .optionText("false")
                .question(question)
                .build();

        // Then
        assertEquals("B", newOption.getOptionLabel());
        assertEquals("false", newOption.getOptionText());
        assertEquals(question, newOption.getQuestion());
    }

    @Test
    void quizAttempt_ShouldHaveCorrectProperties() {
        // Then
        assertEquals(1L, quizAttempt.getId());
        assertEquals(quiz, quizAttempt.getQuiz());
        assertEquals("Test User", quizAttempt.getUserName());
        assertEquals(1, quizAttempt.getScore());
        assertEquals(1, quizAttempt.getTotalQuestions());
        assertNotNull(quizAttempt.getSubmittedAt());
        assertNotNull(quizAttempt.getResponses());
        assertTrue(quizAttempt.getResponses().isEmpty());
    }

    @Test
    void quizAttempt_ShouldAllowAddingResponses() {
        // When
        quizAttempt.getResponses().add(questionResponse);

        // Then
        assertEquals(1, quizAttempt.getResponses().size());
        assertEquals(questionResponse, quizAttempt.getResponses().get(0));
    }

    @Test
    void quizAttempt_ShouldSupportBuilderPattern() {
        // When
        QuizAttempt newAttempt = QuizAttempt.builder()
                .quiz(quiz)
                .userName("Another User")
                .score(3)
                .totalQuestions(5)
                .submittedAt(LocalDateTime.now())
                .build();

        // Then
        assertEquals(quiz, newAttempt.getQuiz());
        assertEquals("Another User", newAttempt.getUserName());
        assertEquals(3, newAttempt.getScore());
        assertEquals(5, newAttempt.getTotalQuestions());
        assertNotNull(newAttempt.getSubmittedAt());
    }

    @Test
    void questionResponse_ShouldHaveCorrectProperties() {
        // Then
        assertEquals(1L, questionResponse.getId());
        assertEquals(question, questionResponse.getQuestion());
        assertEquals(quizAttempt, questionResponse.getQuizAttempt());
        assertEquals("false", questionResponse.getSelectedAnswer());
        assertTrue(questionResponse.getIsCorrect());
        assertEquals("Correct! The default value of a boolean variable in Java is false.", questionResponse.getFeedback());
    }

    @Test
    void questionResponse_ShouldSupportBuilderPattern() {
        // When
        QuestionResponse newResponse = QuestionResponse.builder()
                .question(question)
                .quizAttempt(quizAttempt)
                .selectedAnswer("true")
                .isCorrect(false)
                .feedback("Incorrect answer")
                .build();

        // Then
        assertEquals(question, newResponse.getQuestion());
        assertEquals(quizAttempt, newResponse.getQuizAttempt());
        assertEquals("true", newResponse.getSelectedAnswer());
        assertFalse(newResponse.getIsCorrect());
        assertEquals("Incorrect answer", newResponse.getFeedback());
    }

    @Test
    void entities_ShouldSupportEqualsAndHashCode() {
        // Given
        Quiz quiz1 = Quiz.builder().id(1L).topic("Java").build();
        Quiz quiz2 = Quiz.builder().id(1L).topic("Java").build();
        Quiz quiz3 = Quiz.builder().id(2L).topic("Python").build();

        // Then
        assertEquals(quiz1, quiz2);
        assertNotEquals(quiz1, quiz3);
        assertEquals(quiz1.hashCode(), quiz2.hashCode());
    }

    @Test
    void entities_ShouldSupportToString() {
        // When & Then
        assertNotNull(quiz.toString());
        assertNotNull(question.toString());
        assertNotNull(option.toString());
        assertNotNull(quizAttempt.toString());
        assertNotNull(questionResponse.toString());
        
        assertTrue(quiz.toString().contains("Java"));
        assertTrue(question.toString().contains("boolean"));
        assertTrue(option.toString().contains("true"));
        assertTrue(quizAttempt.toString().contains("Test User"));
        assertTrue(questionResponse.toString().contains("false"));
    }

    @Test
    void quiz_ShouldHandleNullValues() {
        // When
        Quiz quizWithNulls = Quiz.builder()
                .topic(null)
                .title(null)
                .description(null)
                .build();

        // Then
        assertNull(quizWithNulls.getTopic());
        assertNull(quizWithNulls.getTitle());
        assertNull(quizWithNulls.getDescription());
        assertNull(quizWithNulls.getCreatedAt());
        assertNull(quizWithNulls.getUpdatedAt());
    }

    @Test
    void question_ShouldHandleEmptyOptions() {
        // Given
        Question questionWithEmptyOptions = Question.builder()
                .questionText("Test question")
                .options(new ArrayList<>())
                .build();

        // Then
        assertNotNull(questionWithEmptyOptions.getOptions());
        assertTrue(questionWithEmptyOptions.getOptions().isEmpty());
    }

    @Test
    void quizAttempt_ShouldCalculatePercentage() {
        // Given
        QuizAttempt attempt = QuizAttempt.builder()
                .score(3)
                .totalQuestions(5)
                .build();

        // When
        double percentage = (double) attempt.getScore() / attempt.getTotalQuestions() * 100;

        // Then
        assertEquals(60.0, percentage, 0.01);
    }
}
