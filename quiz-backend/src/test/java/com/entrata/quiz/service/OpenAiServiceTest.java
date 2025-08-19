package com.entrata.quiz.service;

import com.entrata.quiz.config.OpenAiConfig;
import com.entrata.quiz.dto.QuizGenerationRequest;
import com.entrata.quiz.entity.Quiz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAiServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private OpenAiConfig openAiConfig;

    @InjectMocks
    private OpenAiService openAiService;

    private QuizGenerationRequest sampleRequest;
    private String sampleOpenAiResponse;

    @BeforeEach
    void setUp() {
        sampleRequest = new QuizGenerationRequest();
        sampleRequest.setTopic("Java");
        
        when(openAiConfig.getApiKey()).thenReturn("test-api-key");
        when(openAiConfig.getModel()).thenReturn("gpt-4o-mini");
        when(openAiConfig.getBaseUrl()).thenReturn("https://api.openai.com/v1");

        sampleOpenAiResponse = """
            {
              "id": "chatcmpl-test123",
              "object": "chat.completion",
              "created": 1234567890,
              "model": "gpt-4o-mini",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "TITLE: Java Fundamentals Quiz\\n\\nDESCRIPTION: A quiz designed to test basic knowledge of Java programming concepts.\\n\\nQUESTION 1:\\nWhat is the main method signature in Java?\\nA) public static void main(String[] args)\\nB) public void main(String[] args)\\nC) static void main(String[] args)\\nD) public main(String[] args)\\nCORRECT: A\\nEXPLANATION: The main method must be public, static, void, and take a String array parameter.\\n\\nQUESTION 2:\\nWhich of the following is a primitive data type in Java?\\nA) String\\nB) Integer\\nC) int\\nD) ArrayList\\nCORRECT: C\\nEXPLANATION: int is a primitive data type, while String, Integer, and ArrayList are reference types.\\n\\nQUESTION 3:\\nWhat keyword is used to create a subclass in Java?\\nA) extends\\nB) implements\\nC) inherits\\nD) derives\\nCORRECT: A\\nEXPLANATION: The extends keyword is used to create inheritance relationships between classes.\\n\\nQUESTION 4:\\nWhich keyword is used to define a class in Java?\\nA) class\\nB) define\\nC) object\\nD) structure\\nCORRECT: A\\nEXPLANATION: The keyword class is used to define a class in Java.\\n\\nQUESTION 5:\\nWhat is the purpose of the static keyword in Java?\\nA) It indicates that a method or variable belongs to the class rather than instances of the class.\\nB) It makes a variable immutable.\\nC) It allows for multiple instances of a class.\\nD) It indicates that a method is abstract.\\nCORRECT: A\\nEXPLANATION: The static keyword means that the method or variable is associated with the class itself rather than any specific instance of the class."
                  },
                  "logprobs": null,
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 150,
                "completion_tokens": 400,
                "total_tokens": 550
              }
            }
            """;
    }

    private void setupWebClientMocks() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    @Test
    void generateQuiz_ShouldReturnQuizWhenSuccessful() {
        // Given
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(sampleOpenAiResponse));

        // When
        Quiz result = openAiService.generateQuiz(sampleRequest);

        // Then
        assertNotNull(result);
        assertEquals("Java", result.getTopic());
        assertEquals("Java Fundamentals Quiz", result.getTitle());
        assertEquals("A quiz designed to test basic knowledge of Java programming concepts.", result.getDescription());
        assertNotNull(result.getQuestions());
        assertEquals(5, result.getQuestions().size());

        // Verify first question
        var firstQuestion = result.getQuestions().get(0);
        assertEquals("What is the main method signature in Java?", firstQuestion.getQuestionText());
        assertEquals("public static void main(String[] args)", firstQuestion.getCorrectAnswer());
        assertEquals(4, firstQuestion.getOptions().size());
        assertEquals("public static void main(String[] args)", firstQuestion.getOptions().get(0).getOptionText());
        
        verify(webClient, times(1)).post();
    }

    @Test
    void buildPrompt_ShouldCreateValidPrompt() {
        // This tests the prompt building logic indirectly through generateQuiz
        // Given
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(sampleOpenAiResponse));

        // When
        openAiService.generateQuiz(sampleRequest);

        // Then - Verify that the API was called with proper parameters
        verify(webClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri("https://api.openai.com/v1/chat/completions");
        verify(requestBodySpec, times(1)).header("Authorization", "Bearer test-api-key");
        verify(requestBodySpec, times(1)).header("Content-Type", "application/json");
    }
}