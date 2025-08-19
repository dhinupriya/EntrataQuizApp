package com.entrata.quiz;

import com.entrata.quiz.entity.Quiz;
import com.entrata.quiz.entity.Question;
import com.entrata.quiz.entity.QuestionOption;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple working test to demonstrate the testing framework setup
 * This test validates basic entity functionality without complex mocking
 */
@SpringBootTest
class SimpleTest {

    @Test
    void contextLoads() {
        // This test ensures the Spring context loads successfully
        assertTrue(true);
    }

    @Test
    void quiz_ShouldCreateWithBuilder() {
        // Test Quiz entity builder pattern
        Quiz quiz = Quiz.builder()
                .topic("Java")
                .title("Java Basics")
                .description("Test your Java knowledge")
                .createdAt(LocalDateTime.now())
                .questions(new ArrayList<>())
                .build();

        assertNotNull(quiz);
        assertEquals("Java", quiz.getTopic());
        assertEquals("Java Basics", quiz.getTitle());
        assertEquals("Test your Java knowledge", quiz.getDescription());
        assertNotNull(quiz.getQuestions());
        assertTrue(quiz.getQuestions().isEmpty());
    }

    @Test
    void question_ShouldCreateWithBuilder() {
        // Test Question entity builder pattern
        Question question = Question.builder()
                .questionText("What is Java?")
                .correctAnswer("Programming Language")
                .explanation("Java is a programming language")
                .questionNumber(1)
                .options(new ArrayList<>())
                .build();

        assertNotNull(question);
        assertEquals("What is Java?", question.getQuestionText());
        assertEquals("Programming Language", question.getCorrectAnswer());
        assertEquals("Java is a programming language", question.getExplanation());
        assertEquals(1, question.getQuestionNumber());
        assertNotNull(question.getOptions());
    }

    @Test
    void questionOption_ShouldCreateWithBuilder() {
        // Test QuestionOption entity builder pattern
        QuestionOption option = QuestionOption.builder()
                .optionLabel("A")
                .optionText("Programming Language")
                .build();

        assertNotNull(option);
        assertEquals("A", option.getOptionLabel());
        assertEquals("Programming Language", option.getOptionText());
    }

    @Test
    void entities_ShouldSupportToString() {
        // Test that entities have proper toString implementations
        Quiz quiz = Quiz.builder()
                .topic("Java")
                .title("Test Quiz")
                .build();

        Question question = Question.builder()
                .questionText("Test question?")
                .correctAnswer("Test answer")
                .build();

        QuestionOption option = QuestionOption.builder()
                .optionLabel("A")
                .optionText("Test option")
                .build();

        String quizString = quiz.toString();
        String questionString = question.toString();
        String optionString = option.toString();

        assertNotNull(quizString);
        assertNotNull(questionString);
        assertNotNull(optionString);
        
        assertTrue(quizString.contains("Java"));
        assertTrue(questionString.contains("Test question"));
        assertTrue(optionString.contains("Test option"));
    }

    @Test
    void quiz_ShouldAllowAddingQuestions() {
        // Test relationship between Quiz and Question
        Quiz quiz = Quiz.builder()
                .topic("Java")
                .questions(new ArrayList<>())
                .build();

        Question question = Question.builder()
                .questionText("What is Java?")
                .quiz(quiz)
                .options(new ArrayList<>())
                .build();

        quiz.getQuestions().add(question);

        assertEquals(1, quiz.getQuestions().size());
        assertEquals(question, quiz.getQuestions().get(0));
        assertEquals(quiz, question.getQuiz());
    }

    @Test
    void question_ShouldAllowAddingOptions() {
        // Test relationship between Question and QuestionOption
        Question question = Question.builder()
                .questionText("What is Java?")
                .options(new ArrayList<>())
                .build();

        QuestionOption option = QuestionOption.builder()
                .optionLabel("A")
                .optionText("Programming Language")
                .question(question)
                .build();

        question.getOptions().add(option);

        assertEquals(1, question.getOptions().size());
        assertEquals(option, question.getOptions().get(0));
        assertEquals(question, option.getQuestion());
    }
}
