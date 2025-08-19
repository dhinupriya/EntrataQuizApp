package com.entrata.quiz.integration;

import com.entrata.quiz.dto.QuizSubmissionRequest;
import com.entrata.quiz.entity.Quiz;
import com.entrata.quiz.entity.Question;
import com.entrata.quiz.entity.QuestionOption;
import com.entrata.quiz.entity.QuizAttempt;
import com.entrata.quiz.repository.QuizRepository;
import com.entrata.quiz.repository.QuizAttemptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizSubmissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    private Quiz sampleQuiz;
    private QuizSubmissionRequest validSubmissionRequest;

    @BeforeEach
    void setUp() {
        // Clean up database
        quizAttemptRepository.deleteAll();
        quizRepository.deleteAll();
        
        // Create and save sample quiz
        sampleQuiz = createSampleQuiz();
        sampleQuiz = quizRepository.save(sampleQuiz);
        
        // Create valid submission request
        validSubmissionRequest = new QuizSubmissionRequest();
        validSubmissionRequest.setQuizId(sampleQuiz.getId());
        validSubmissionRequest.setUserName("testUser");
        
        List<QuizSubmissionRequest.QuestionAnswer> answers = new ArrayList<>();
        // Provide correct answers for both questions
        answers.add(createQuestionAnswer(sampleQuiz.getQuestions().get(0).getId(), "A programming language"));
        answers.add(createQuestionAnswer(sampleQuiz.getQuestions().get(1).getId(), "Java Virtual Machine"));
        validSubmissionRequest.setAnswers(answers);
    }

    @Test
    void submitQuiz_WithCorrectAnswers_ShouldReturnFullScore() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSubmissionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.feedback", hasSize(2)))
                .andExpect(jsonPath("$.feedback[0].correct").value(true))
                .andExpect(jsonPath("$.feedback[1].correct").value(true))
                .andExpect(jsonPath("$.feedback[0].explanation").exists())
                .andExpect(jsonPath("$.feedback[1].explanation").exists());
    }

    @Test
    void submitQuiz_WithIncorrectAnswers_ShouldReturnZeroScore() throws Exception {
        // Given - provide wrong answers
        List<QuizSubmissionRequest.QuestionAnswer> wrongAnswers = new ArrayList<>();
        wrongAnswers.add(createQuestionAnswer(sampleQuiz.getQuestions().get(0).getId(), "A database"));
        wrongAnswers.add(createQuestionAnswer(sampleQuiz.getQuestions().get(1).getId(), "Java Version Manager"));
        validSubmissionRequest.setAnswers(wrongAnswers);

        // When & Then
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSubmissionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.feedback", hasSize(2)))
                .andExpect(jsonPath("$.feedback[0].correct").value(false))
                .andExpect(jsonPath("$.feedback[1].correct").value(false));
    }

    @Test
    void submitQuiz_WithMixedAnswers_ShouldReturnPartialScore() throws Exception {
        // Given - provide one correct, one incorrect answer
        List<QuizSubmissionRequest.QuestionAnswer> mixedAnswers = new ArrayList<>();
        mixedAnswers.add(createQuestionAnswer(sampleQuiz.getQuestions().get(0).getId(), "A programming language")); // Correct
        mixedAnswers.add(createQuestionAnswer(sampleQuiz.getQuestions().get(1).getId(), "Java Version Manager")); // Incorrect
        validSubmissionRequest.setAnswers(mixedAnswers);

        // When & Then
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSubmissionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.feedback", hasSize(2)))
                .andExpect(jsonPath("$.feedback[0].correct").value(true))
                .andExpect(jsonPath("$.feedback[1].correct").value(false));
    }

    @Test
    void submitQuiz_WithInvalidQuizId_ShouldReturnBadRequest() throws Exception {
        // Given
        validSubmissionRequest.setQuizId(999L);

        // When & Then
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSubmissionRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitQuiz_WithMissingUserName_ShouldReturnBadRequest() throws Exception {
        // Given
        validSubmissionRequest.setUserName("");

        // When & Then
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSubmissionRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitQuiz_WithMissingAnswers_ShouldReturnBadRequest() throws Exception {
        // Given
        validSubmissionRequest.setAnswers(new ArrayList<>());

        // When & Then
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSubmissionRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitQuiz_WithInvalidQuestionId_ShouldReturnBadRequest() throws Exception {
        // Given
        List<QuizSubmissionRequest.QuestionAnswer> invalidAnswers = new ArrayList<>();
        invalidAnswers.add(createQuestionAnswer(999L, "Some answer")); // Invalid question ID
        validSubmissionRequest.setAnswers(invalidAnswers);

        // When & Then
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSubmissionRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserQuizHistory_ShouldReturnUserAttempts() throws Exception {
        // Given - create some quiz attempts
        createQuizAttempt("testUser", 2, 2);
        createQuizAttempt("testUser", 1, 2);
        createQuizAttempt("anotherUser", 2, 2); // This should not be included

        // When & Then
        mockMvc.perform(get("/api/quiz-submissions/user/{userName}/history", "testUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userName").value("testUser"))
                .andExpect(jsonPath("$[1].userName").value("testUser"));
    }

    @Test
    void getUserQuizHistory_WithNoHistory_ShouldReturnEmptyArray() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/quiz-submissions/user/{userName}/history", "nonExistentUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getQuizAttempts_ShouldReturnAllAttemptsForQuiz() throws Exception {
        // Given - create some quiz attempts
        createQuizAttempt("user1", 2, 2);
        createQuizAttempt("user2", 1, 2);
        createQuizAttempt("user3", 0, 2);

        // When & Then
        mockMvc.perform(get("/api/quiz-submissions/quiz/{quizId}/attempts", sampleQuiz.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].quizId").value(sampleQuiz.getId().intValue()))
                .andExpect(jsonPath("$[1].quizId").value(sampleQuiz.getId().intValue()))
                .andExpect(jsonPath("$[2].quizId").value(sampleQuiz.getId().intValue()));
    }

    @Test
    void getQuizAttempts_WithNoAttempts_ShouldReturnEmptyArray() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/quiz-submissions/quiz/{quizId}/attempts", sampleQuiz.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getQuizAttempts_WithInvalidQuizId_ShouldReturnEmptyArray() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/quiz-submissions/quiz/{quizId}/attempts", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private Quiz createSampleQuiz() {
        Quiz quiz = Quiz.builder()
                .topic("Java Programming")
                .title("Java Fundamentals Quiz")
                .description("Test your basic Java knowledge")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .questions(new ArrayList<>())
                .build();

        // Create questions
        Question question1 = Question.builder()
                .questionText("What is Java?")
                .correctAnswer("A programming language")
                .explanation("Java is a high-level, object-oriented programming language.")
                .questionNumber(1)
                .quiz(quiz)
                .options(new ArrayList<>())
                .build();

        Question question2 = Question.builder()
                .questionText("What is JVM?")
                .correctAnswer("Java Virtual Machine")
                .explanation("JVM is the runtime environment for Java applications.")
                .questionNumber(2)
                .quiz(quiz)
                .options(new ArrayList<>())
                .build();

        // Create options for question1
        List<QuestionOption> options1 = List.of(
                QuestionOption.builder().optionLabel("A").optionText("A programming language").question(question1).build(),
                QuestionOption.builder().optionLabel("B").optionText("A database").question(question1).build(),
                QuestionOption.builder().optionLabel("C").optionText("An operating system").question(question1).build(),
                QuestionOption.builder().optionLabel("D").optionText("A web browser").question(question1).build()
        );

        // Create options for question2
        List<QuestionOption> options2 = List.of(
                QuestionOption.builder().optionLabel("A").optionText("Java Virtual Machine").question(question2).build(),
                QuestionOption.builder().optionLabel("B").optionText("Java Version Manager").question(question2).build(),
                QuestionOption.builder().optionLabel("C").optionText("Java Variable Method").question(question2).build(),
                QuestionOption.builder().optionLabel("D").optionText("Java Visual Mode").question(question2).build()
        );

        question1.setOptions(options1);
        question2.setOptions(options2);
        quiz.setQuestions(List.of(question1, question2));

        return quiz;
    }

    private QuizSubmissionRequest.QuestionAnswer createQuestionAnswer(Long questionId, String selectedAnswer) {
        QuizSubmissionRequest.QuestionAnswer answer = new QuizSubmissionRequest.QuestionAnswer();
        answer.setQuestionId(questionId);
        answer.setSelectedAnswer(selectedAnswer);
        return answer;
    }

    private void createQuizAttempt(String userName, int score, int totalQuestions) {
        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(sampleQuiz)
                .userName(userName)
                .score(score)
                .totalQuestions(totalQuestions)
                .submittedAt(LocalDateTime.now())
                .responses(new ArrayList<>())
                .build();
        quizAttemptRepository.save(attempt);
    }
}
