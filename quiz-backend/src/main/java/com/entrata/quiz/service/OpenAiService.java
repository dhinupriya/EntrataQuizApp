package com.entrata.quiz.service;

import com.entrata.quiz.config.OpenAiConfig;
import com.entrata.quiz.dto.QuizGenerationRequest;
import com.entrata.quiz.entity.Question;
import com.entrata.quiz.entity.QuestionOption;
import com.entrata.quiz.entity.Quiz;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiService {
    
    private final OpenAiConfig openAiConfig;
    private final WebClient webClient;
    
    public Quiz generateQuiz(QuizGenerationRequest request) {
        try {
            // Validate OpenAI configuration
            if (openAiConfig.getApiKey() == null || openAiConfig.getApiKey().trim().isEmpty()) {
                log.error("OpenAI API key is not configured");
                throw new RuntimeException("OpenAI API key is not configured. Please set OPENAI_API_KEY environment variable.");
            }
            
            if (openAiConfig.getModel() == null || openAiConfig.getModel().trim().isEmpty()) {
                log.error("OpenAI model is not configured");
                throw new RuntimeException("OpenAI model is not configured.");
            }
            
            if (openAiConfig.getBaseUrl() == null || openAiConfig.getBaseUrl().trim().isEmpty()) {
                log.error("OpenAI base URL is not configured");
                throw new RuntimeException("OpenAI base URL is not configured.");
            }
            
            log.info("Generating quiz for topic: {} using model: {}", request.getTopic(), openAiConfig.getModel());
            
            String prompt = buildPrompt(request);
            String response = callOpenAi(prompt);
            return parseQuizResponse(response, request);
        } catch (Exception e) {
            log.error("Error generating quiz for topic: {}", request.getTopic(), e);
            throw new RuntimeException("Failed to generate quiz: " + e.getMessage(), e);
        }
    }
    
    private String buildPrompt(QuizGenerationRequest request) {
        return String.format("""
            Create a quiz with exactly 5 multiple choice questions about: %s
            
            Format your response exactly like this:
            
            TITLE: [Quiz Title]
            DESCRIPTION: [Brief description]
            
            QUESTION 1:
            [Question text]
            A) [Option A]
            B) [Option B]
            C) [Option C]
            D) [Option D]
            CORRECT: [A, B, C, or D]
            EXPLANATION: [Why this is correct]
            
            QUESTION 2:
            [Question text]
            A) [Option A]
            B) [Option B]
            C) [Option C]
            D) [Option D]
            CORRECT: [A, B, C, or D]
            EXPLANATION: [Why this is correct]
            
            Continue for all 5 questions. Each question must have exactly 4 options labeled A, B, C, D, and exactly one correct answer.
            """, request.getTopic());
    }
    
    private String callOpenAi(String prompt) {
        try {
            // Escape the prompt content to avoid JSON issues
            String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");
            
            String requestBody = String.format("""
                {
                    "model": "%s",
                    "messages": [
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ],
                    "max_tokens": 2000,
                    "temperature": 0.7
                }
                """, openAiConfig.getModel(), escapedPrompt);
            
            log.debug("Calling OpenAI API with model: {}, base URL: {}", openAiConfig.getModel(), openAiConfig.getBaseUrl());
            log.debug("Request body: {}", requestBody);
            
            String response = webClient.post()
                    .uri(openAiConfig.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + openAiConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("OpenAI API error response: {}", errorBody);
                                        return Mono.error(new RuntimeException("OpenAI API error: " + errorBody));
                                    }))
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("OpenAI API returned empty response");
            }
            
            log.debug("OpenAI API response received, length: {}", response.length());
            // Clean up the response by unescaping newlines and other escape sequences
            return cleanResponseText(response);
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }
    
    private Quiz parseQuizResponse(String response, QuizGenerationRequest request) {
        // Extract title and description
        String title = extractValue(response, "TITLE:", "DESCRIPTION:");
        String description = extractValue(response, "DESCRIPTION:", "QUESTION 1:");
        
        Quiz quiz = Quiz.builder()
                .topic(request.getTopic())
                .title(title != null ? title.trim() : "Quiz on " + request.getTopic())
                .description(description != null ? description.trim() : "")
                .questions(new ArrayList<>())
                .build();
        
        // Parse questions
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Question question = parseQuestion(response, i, quiz);
            if (question != null) {
                questions.add(question);
            }
        }
        
        quiz.setQuestions(questions);
        return quiz;
    }
    
    private Question parseQuestion(String response, int questionNumber, Quiz quiz) {
        String questionSection = extractQuestionSection(response, questionNumber);
        if (questionSection == null) return null;
        
        String questionText = extractValue(questionSection, "", "A)");
        String optionA = extractValue(questionSection, "A)", "B)");
        String optionB = extractValue(questionSection, "B)", "C)");
        String optionC = extractValue(questionSection, "C)", "D)");
        String optionD = extractValue(questionSection, "D)", "CORRECT:");
        String correctAnswerRaw = extractValue(questionSection, "CORRECT:", "EXPLANATION:");
        String explanation = extractValue(questionSection, "EXPLANATION:", "");
        
        // Clean up the correct answer to extract just the option label (A, B, C, or D)
        String correctAnswer = cleanCorrectAnswer(correctAnswerRaw);
        
        if (questionText == null || optionA == null || optionB == null || 
            optionC == null || optionD == null || correctAnswer == null) {
            return null;
        }
        
        Question question = Question.builder()
                .questionText(questionText.trim())
                .correctAnswer(correctAnswer.trim())
                .explanation(explanation != null ? cleanExplanationText(explanation.trim()) : "")
                .quiz(quiz)
                .questionNumber(questionNumber)
                .options(new ArrayList<>())
                .build();
        
        // Create options
        List<QuestionOption> options = new ArrayList<>();
        options.add(QuestionOption.builder().optionLabel("A").optionText(optionA.trim()).question(question).build());
        options.add(QuestionOption.builder().optionLabel("B").optionText(optionB.trim()).question(question).build());
        options.add(QuestionOption.builder().optionLabel("C").optionText(optionC.trim()).question(question).build());
        options.add(QuestionOption.builder().optionLabel("D").optionText(optionD.trim()).question(question).build());
        
        question.setOptions(options);
        return question;
    }
    
    private String extractValue(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start == -1) return null;
        
        start += startMarker.length();
        int end = endMarker.isEmpty() ? text.length() : text.indexOf(endMarker, start);
        
        if (end == -1) end = text.length();
        return text.substring(start, end).trim();
    }
    
    private String extractQuestionSection(String response, int questionNumber) {
        String startMarker = "QUESTION " + questionNumber + ":";
        String endMarker = questionNumber < 5 ? "QUESTION " + (questionNumber + 1) + ":" : "";
        
        int start = response.indexOf(startMarker);
        if (start == -1) return null;
        
        start += startMarker.length();
        int end = endMarker.isEmpty() ? response.length() : response.indexOf(endMarker, start);
        
        if (end == -1) end = response.length();
        return response.substring(start, end).trim();
    }
    
    /**
     * Clean up the response text by unescaping common escape sequences
     * that might be returned by the OpenAI API
     */
    private String cleanResponseText(String response) {
        if (response == null) return null;
        
        return response
                .replace("\\n", "\n")           // Convert escaped newlines to actual newlines
                .replace("\\t", "\t")           // Convert escaped tabs to actual tabs
                .replace("\\\"", "\"")          // Convert escaped quotes to actual quotes
                .replace("\\\\", "\\")          // Convert escaped backslashes to actual backslashes
                .trim();                        // Remove leading/trailing whitespace
    }
    
    /**
     * Clean up the correct answer to extract just the option label (A, B, C, or D)
     */
    private String cleanCorrectAnswer(String correctAnswerRaw) {
        if (correctAnswerRaw == null) return null;
        
        // Remove any extra text and extract just the option label
        String cleaned = correctAnswerRaw.trim();
        
        // Look for A, B, C, or D in the text
        if (cleaned.contains("A") || cleaned.equalsIgnoreCase("A")) return "A";
        if (cleaned.contains("B") || cleaned.equalsIgnoreCase("B")) return "B";
        if (cleaned.contains("C") || cleaned.equalsIgnoreCase("C")) return "C";
        if (cleaned.contains("D") || cleaned.equalsIgnoreCase("D")) return "D";
        
        // If no clear label found, return the original text (fallback)
        return cleaned;
    }
    
    /**
     * Clean up the explanation text by removing unwanted prefixes and formatting
     */
    private String cleanExplanationText(String explanation) {
        if (explanation == null) return null;
        
        String cleaned = explanation.trim();
        
        // Remove common unwanted prefixes
        if (cleaned.startsWith("Explanation:")) {
            cleaned = cleaned.substring("Explanation:".length()).trim();
        }
        if (cleaned.startsWith("EXPLANATION:")) {
            cleaned = cleaned.substring("EXPLANATION:".length()).trim();
        }
        if (cleaned.startsWith("Explanation")) {
            cleaned = cleaned.substring("Explanation".length()).trim();
        }
        if (cleaned.startsWith("EXPLANATION")) {
            cleaned = cleaned.substring("EXPLANATION".length()).trim();
        }
        
        // Remove any leading colons or spaces
        cleaned = cleaned.replaceAll("^[:\\s]+", "");
        
        // Remove emojis and other special characters that might cause display issues
        cleaned = cleaned.replaceAll("[\\p{So}\\p{Sk}]", ""); // Remove emojis and symbols
        cleaned = cleaned.replaceAll("🎉", ""); // Remove specific emojis
        cleaned = cleaned.replaceAll("✅", "");
        cleaned = cleaned.replaceAll("❌", "");
        cleaned = cleaned.replaceAll("💡", "");
        
        // Clean up any extra whitespace
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
}
