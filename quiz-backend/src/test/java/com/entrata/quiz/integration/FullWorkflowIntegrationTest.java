package com.entrata.quiz.integration;

import com.entrata.quiz.dto.QuizGenerationRequest;
import com.entrata.quiz.dto.QuizSubmissionRequest;
import com.entrata.quiz.entity.Quiz;
import com.entrata.quiz.entity.Question;
import com.entrata.quiz.entity.QuestionOption;
import com.entrata.quiz.repository.QuizRepository;
import com.entrata.quiz.repository.QuizAttemptRepository;
import com.entrata.quiz.service.OpenAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FullWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @MockBean
    private OpenAiService openAiService;

    @BeforeEach
    void setUp() {
        // Clean up database
        quizAttemptRepository.deleteAll();
        quizRepository.deleteAll();
    }

    @Test
    void completeQuizWorkflow_ShouldWorkEndToEnd() throws Exception {
        // Step 1: Generate a quiz
        QuizGenerationRequest generationRequest = new QuizGenerationRequest();
        generationRequest.setTopic("Java Programming");
        generationRequest.setDescription("Basic Java concepts");

        Quiz mockQuiz = createSampleQuiz();
        when(openAiService.generateQuiz(any(QuizGenerationRequest.class)))
                .thenReturn(mockQuiz);

        MvcResult generateResult = mockMvc.perform(post("/api/quizzes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("Java Programming"))
                .andExpect(jsonPath("$.questions", hasSize(2)))
                .andReturn();

        // Extract quiz ID from response
        String generateResponse = generateResult.getResponse().getContentAsString();
        Long quizId = objectMapper.readTree(generateResponse).get("id").asLong();

        // Step 2: Retrieve the generated quiz
        mockMvc.perform(get("/api/quizzes/{id}", quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quizId))
                .andExpect(jsonPath("$.topic").value("Java Programming"))
                .andExpect(jsonPath("$.questions", hasSize(2)));

        // Step 3: Submit quiz answers
        QuizSubmissionRequest submissionRequest = new QuizSubmissionRequest();
        submissionRequest.setQuizId(quizId);
        submissionRequest.setUserName("integrationTestUser");

        // Get question IDs from the database (since we can't easily extract from JSON in this context)
        Quiz savedQuiz = quizRepository.findById(quizId).orElseThrow();
        List<QuizSubmissionRequest.QuestionAnswer> answers = new ArrayList<>();
        
        // Provide correct answers
        for (Question question : savedQuiz.getQuestions()) {
            QuizSubmissionRequest.QuestionAnswer answer = new QuizSubmissionRequest.QuestionAnswer();
            answer.setQuestionId(question.getId());
            answer.setSelectedAnswer(question.getCorrectAnswer());
            answers.add(answer);
        }
        submissionRequest.setAnswers(answers);

        // Submit the quiz
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submissionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.totalQuestions").value(2))
                .andExpect(jsonPath("$.feedback", hasSize(2)))
                .andExpect(jsonPath("$.feedback[0].correct").value(true))
                .andExpect(jsonPath("$.feedback[1].correct").value(true));

        // Step 4: Check user quiz history
        mockMvc.perform(get("/api/quiz-submissions/user/{userName}/history", "integrationTestUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userName").value("integrationTestUser"))
                .andExpect(jsonPath("$[0].score").value(2));

        // Step 5: Check quiz attempts
        mockMvc.perform(get("/api/quiz-submissions/quiz/{quizId}/attempts", quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quizId").value(quizId.intValue()))
                .andExpect(jsonPath("$[0].userName").value("integrationTestUser"));

        // Step 6: Search for quizzes
        mockMvc.perform(get("/api/quizzes/search")
                .param("topic", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].topic").value("Java Programming"));

        // Step 7: Get all quizzes
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(quizId));

        // Step 8: Check configuration
        mockMvc.perform(get("/api/quizzes/config/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Configuration loaded"))
                .andExpect(jsonPath("$.apiKeyConfigured").value(true));

        // Step 9: Delete the quiz (cleanup)
        mockMvc.perform(delete("/api/quizzes/{id}", quizId))
                .andExpect(status().isNoContent());

        // Step 10: Verify deletion
        mockMvc.perform(get("/api/quizzes/{id}", quizId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void multipleUsersQuizWorkflow_ShouldIsolateUserData() throws Exception {
        // Step 1: Generate a quiz
        QuizGenerationRequest generationRequest = new QuizGenerationRequest();
        generationRequest.setTopic("Python Programming");

        Quiz mockQuiz = createSampleQuiz();
        mockQuiz.setTopic("Python Programming");
        mockQuiz.setTitle("Python Basics Quiz");
        
        when(openAiService.generateQuiz(any(QuizGenerationRequest.class)))
                .thenReturn(mockQuiz);

        MvcResult generateResult = mockMvc.perform(post("/api/quizzes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(generationRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract quiz ID
        String generateResponse = generateResult.getResponse().getContentAsString();
        Long quizId = objectMapper.readTree(generateResponse).get("id").asLong();

        Quiz savedQuiz = quizRepository.findById(quizId).orElseThrow();

        // Step 2: User 1 submits quiz with correct answers
        QuizSubmissionRequest user1Submission = createSubmissionRequest(quizId, "user1", savedQuiz, true);
        
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1Submission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(2));

        // Step 3: User 2 submits quiz with incorrect answers
        QuizSubmissionRequest user2Submission = createSubmissionRequest(quizId, "user2", savedQuiz, false);
        
        mockMvc.perform(post("/api/quiz-submissions/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2Submission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0));

        // Step 4: Check user1 history (should only see their attempt)
        mockMvc.perform(get("/api/quiz-submissions/user/{userName}/history", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userName").value("user1"))
                .andExpect(jsonPath("$[0].score").value(2));

        // Step 5: Check user2 history (should only see their attempt)
        mockMvc.perform(get("/api/quiz-submissions/user/{userName}/history", "user2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userName").value("user2"))
                .andExpect(jsonPath("$[0].score").value(0));

        // Step 6: Check all attempts for the quiz (should see both)
        mockMvc.perform(get("/api/quiz-submissions/quiz/{quizId}/attempts", quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
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

    private QuizSubmissionRequest createSubmissionRequest(Long quizId, String userName, Quiz quiz, boolean correctAnswers) {
        QuizSubmissionRequest request = new QuizSubmissionRequest();
        request.setQuizId(quizId);
        request.setUserName(userName);

        List<QuizSubmissionRequest.QuestionAnswer> answers = new ArrayList<>();
        for (Question question : quiz.getQuestions()) {
            QuizSubmissionRequest.QuestionAnswer answer = new QuizSubmissionRequest.QuestionAnswer();
            answer.setQuestionId(question.getId());
            
            if (correctAnswers) {
                answer.setSelectedAnswer(question.getCorrectAnswer());
            } else {
                // Select the first wrong option
                answer.setSelectedAnswer(question.getOptions().get(1).getOptionText());
            }
            answers.add(answer);
        }
        request.setAnswers(answers);
        return request;
    }
}
