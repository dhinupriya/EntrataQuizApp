package com.entrata.quiz.integration;

import com.entrata.quiz.dto.QuizGenerationRequest;
import com.entrata.quiz.dto.QuizResponse;
import com.entrata.quiz.entity.Quiz;
import com.entrata.quiz.entity.Question;
import com.entrata.quiz.entity.QuestionOption;
import com.entrata.quiz.repository.QuizRepository;
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
class QuizControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuizRepository quizRepository;

    @MockBean
    private OpenAiService openAiService;

    private Quiz sampleQuiz;
    private QuizGenerationRequest validRequest;

    @BeforeEach
    void setUp() {
        // Clean up database
        quizRepository.deleteAll();
        
        // Create sample quiz
        sampleQuiz = createSampleQuiz();
        
        // Create valid request
        validRequest = new QuizGenerationRequest();
        validRequest.setTopic("Java Programming");
        validRequest.setDescription("Basic Java concepts");
    }

    @Test
    void generateQuiz_ShouldCreateQuizSuccessfully() throws Exception {
        // Given
        when(openAiService.generateQuiz(any(QuizGenerationRequest.class)))
                .thenReturn(sampleQuiz);

        // When & Then
        mockMvc.perform(post("/api/quizzes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("Java Programming"))
                .andExpect(jsonPath("$.title").value("Java Fundamentals Quiz"))
                .andExpect(jsonPath("$.description").value("Test your basic Java knowledge"))
                .andExpect(jsonPath("$.questions").isArray())
                .andExpect(jsonPath("$.questions", hasSize(2)))
                .andExpect(jsonPath("$.questions[0].questionText").value("What is Java?"))
                .andExpect(jsonPath("$.questions[0].options", hasSize(4)))
                .andExpect(jsonPath("$.questions[0].options[0].optionLabel").value("A"))
                .andExpect(jsonPath("$.questions[0].options[0].optionText").value("A programming language"));
    }

    @Test
    void generateQuiz_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        QuizGenerationRequest invalidRequest = new QuizGenerationRequest();
        invalidRequest.setTopic(""); // Invalid: empty topic

        // When & Then
        mockMvc.perform(post("/api/quizzes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateQuiz_WithTooShortTopic_ShouldReturnBadRequest() throws Exception {
        // Given
        QuizGenerationRequest invalidRequest = new QuizGenerationRequest();
        invalidRequest.setTopic("AB"); // Invalid: too short

        // When & Then
        mockMvc.perform(post("/api/quizzes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getQuizById_WhenQuizExists_ShouldReturnQuiz() throws Exception {
        // Given
        Quiz savedQuiz = quizRepository.save(sampleQuiz);

        // When & Then
        mockMvc.perform(get("/api/quizzes/{id}", savedQuiz.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedQuiz.getId()))
                .andExpect(jsonPath("$.topic").value("Java Programming"))
                .andExpect(jsonPath("$.title").value("Java Fundamentals Quiz"))
                .andExpect(jsonPath("$.questions", hasSize(2)));
    }

    @Test
    void getQuizById_WhenQuizDoesNotExist_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/quizzes/{id}", 999L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllQuizzes_ShouldReturnAllQuizzes() throws Exception {
        // Given
        Quiz quiz1 = createSampleQuiz();
        quiz1.setTopic("Java");
        Quiz quiz2 = createSampleQuiz();
        quiz2.setTopic("Python");
        quiz2.setTitle("Python Basics Quiz");
        
        quizRepository.save(quiz1);
        quizRepository.save(quiz2);

        // When & Then
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].topic").value("Python")) // Ordered by createdAt desc
                .andExpect(jsonPath("$[1].topic").value("Java"));
    }

    @Test
    void getAllQuizzes_WhenNoQuizzes_ShouldReturnEmptyArray() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/quizzes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchQuizzesByTopic_ShouldReturnMatchingQuizzes() throws Exception {
        // Given
        Quiz javaQuiz = createSampleQuiz();
        javaQuiz.setTopic("Java Programming");
        Quiz pythonQuiz = createSampleQuiz();
        pythonQuiz.setTopic("Python Programming");
        pythonQuiz.setTitle("Python Basics Quiz");
        
        quizRepository.save(javaQuiz);
        quizRepository.save(pythonQuiz);

        // When & Then
        mockMvc.perform(get("/api/quizzes/search")
                .param("topic", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].topic").value("Java Programming"));
    }

    @Test
    void searchQuizzesByTopic_WithNoMatches_ShouldReturnEmptyArray() throws Exception {
        // Given
        Quiz javaQuiz = createSampleQuiz();
        quizRepository.save(javaQuiz);

        // When & Then
        mockMvc.perform(get("/api/quizzes/search")
                .param("topic", "NonExistentTopic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void checkConfiguration_ShouldReturnConfigInfo() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/quizzes/config/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Configuration loaded"))
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.baseUrl").value("https://api.openai.com/v1"))
                .andExpect(jsonPath("$.apiKeyConfigured").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deleteQuiz_WhenQuizExists_ShouldDeleteSuccessfully() throws Exception {
        // Given
        Quiz savedQuiz = quizRepository.save(sampleQuiz);

        // When & Then
        mockMvc.perform(delete("/api/quizzes/{id}", savedQuiz.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/quizzes/{id}", savedQuiz.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteQuiz_WhenQuizDoesNotExist_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/quizzes/{id}", 999L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateQuiz_WithOpenAiServiceError_ShouldReturnBadRequest() throws Exception {
        // Given
        when(openAiService.generateQuiz(any(QuizGenerationRequest.class)))
                .thenThrow(new RuntimeException("OpenAI API error"));

        // When & Then
        mockMvc.perform(post("/api/quizzes/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
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
}
