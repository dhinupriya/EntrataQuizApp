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

            CRITICAL REQUIREMENTS:
            - The CORRECT answer MUST be one of the provided options (A, B, C, or D) and match its text EXACTLY.
            - The EXPLANATION MUST clearly and accurately support the CORRECT answer.
            - For programming code questions, the explanation MUST include a step-by-step breakdown of how the code evaluates to the correct answer.

            QUALITY STANDARDS:
            - Questions should be clear, unambiguous, and directly related to the topic.
            - Options should be plausible but only one should be definitively correct.
            - Explanations should be educational, concise, and easy to understand.
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
            
            // Parse JSON response to extract content
            String content = extractContentFromJsonResponse(response);
            
            // Clean up the content by unescaping newlines and other escape sequences
            return cleanResponseText(content);
            
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
        if (questionSection == null) {
            log.error("Could not extract question section for question {}", questionNumber);
            return null;
        }
        
        log.debug("Question {} section: {}", questionNumber, questionSection);
        
        String questionText = extractValue(questionSection, "", "A)");
        String optionA = extractValue(questionSection, "A)", "B)");
        String optionB = extractValue(questionSection, "B)", "C)");
        String optionC = extractValue(questionSection, "C)", "D)");
        String optionD = extractValue(questionSection, "D)", "CORRECT:");
        String correctAnswerRaw = extractValue(questionSection, "CORRECT:", "EXPLANATION:");
        String explanation = extractValue(questionSection, "EXPLANATION:", "");
        
        log.debug("Question {} parsed - Text: '{}', A: '{}', B: '{}', C: '{}', D: '{}', Correct: '{}', Explanation: '{}'", 
                questionNumber, questionText, optionA, optionB, optionC, optionD, correctAnswerRaw, explanation);
        
        // Validate that all required fields are present BEFORE processing
        if (questionText == null || optionA == null || optionB == null || 
            optionC == null || optionD == null) {
            log.error("Question {} parsing failed - missing required fields - Text: {}, A: {}, B: {}, C: {}, D: {}", 
                    questionNumber, questionText, optionA, optionB, optionC, optionD);
            
            // Try fallback parsing with different format
            return parseQuestionFallback(questionSection, questionNumber, quiz);
        }
        
        // Clean up the correct answer to extract just the option label (A, B, C, or D)
        String correctAnswer = cleanCorrectAnswer(correctAnswerRaw, optionA, optionB, optionC, optionD);
        
        // Validate that the correct answer is one of the actual options
        List<String> validOptions = List.of(
            optionA != null ? optionA.trim() : "", 
            optionB != null ? optionB.trim() : "", 
            optionC != null ? optionC.trim() : "", 
            optionD != null ? optionD.trim() : ""
        );
        if (!validOptions.contains(correctAnswer)) {
            log.warn("Correct answer '{}' is not in valid options: {}", correctAnswer, validOptions);
            // Fallback: use the first non-null option as correct answer
            correctAnswer = optionA != null ? optionA.trim() : "Option A";
        }
        
        Question question = Question.builder()
                .questionText(questionText != null ? questionText.trim() : "Question")
                .correctAnswer(correctAnswer != null ? correctAnswer.trim() : "A")
                .explanation(explanation != null ? cleanExplanationText(explanation.trim()) : "")
                .quiz(quiz)
                .questionNumber(questionNumber)
                .options(new ArrayList<>())
                .build();
        
        // Create options (with null safety)
        List<QuestionOption> options = new ArrayList<>();
        options.add(QuestionOption.builder().optionLabel("A").optionText(optionA != null ? optionA.trim() : "Option A").question(question).build());
        options.add(QuestionOption.builder().optionLabel("B").optionText(optionB != null ? optionB.trim() : "Option B").question(question).build());
        options.add(QuestionOption.builder().optionLabel("C").optionText(optionC != null ? optionC.trim() : "Option C").question(question).build());
        options.add(QuestionOption.builder().optionLabel("D").optionText(optionD != null ? optionD.trim() : "Option D").question(question).build());
        
        question.setOptions(options);
        return question;
    }
    
    /**
     * Fallback parsing method for different AI response formats
     */
    private Question parseQuestionFallback(String questionSection, int questionNumber, Quiz quiz) {
        log.info("Attempting fallback parsing for question {}", questionNumber);
        
        // Try to extract using different patterns
        String[] lines = questionSection.split("\n");
        String questionText = null;
        String[] options = new String[4];
        String correctAnswer = null;
        String explanation = "";
        
        int optionIndex = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (questionText == null && !line.startsWith("A") && !line.startsWith("B") && 
                !line.startsWith("C") && !line.startsWith("D") && !line.startsWith("CORRECT") && 
                !line.startsWith("EXPLANATION")) {
                questionText = line;
            } else if (line.startsWith("A") && optionIndex < 4) {
                options[optionIndex++] = line.substring(line.indexOf(")") + 1).trim();
            } else if (line.startsWith("B") && optionIndex < 4) {
                options[optionIndex++] = line.substring(line.indexOf(")") + 1).trim();
            } else if (line.startsWith("C") && optionIndex < 4) {
                options[optionIndex++] = line.substring(line.indexOf(")") + 1).trim();
            } else if (line.startsWith("D") && optionIndex < 4) {
                options[optionIndex++] = line.substring(line.indexOf(")") + 1).trim();
            } else if (line.startsWith("CORRECT")) {
                correctAnswer = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("EXPLANATION")) {
                explanation = line.substring(line.indexOf(":") + 1).trim();
            }
        }
        
        // If we still don't have a correct answer, use the first option
        if (correctAnswer == null && options[0] != null) {
            correctAnswer = options[0];
        }
        
        if (questionText != null && options[0] != null && options[1] != null && 
            options[2] != null && options[3] != null && correctAnswer != null) {
            
            Question question = Question.builder()
                    .questionText(questionText)
                    .correctAnswer(correctAnswer)
                    .explanation(cleanExplanationText(explanation))
                    .quiz(quiz)
                    .questionNumber(questionNumber)
                    .options(new ArrayList<>())
                    .build();
            
            // Create options
            List<QuestionOption> questionOptions = new ArrayList<>();
            questionOptions.add(QuestionOption.builder().optionLabel("A").optionText(options[0]).question(question).build());
            questionOptions.add(QuestionOption.builder().optionLabel("B").optionText(options[1]).question(question).build());
            questionOptions.add(QuestionOption.builder().optionLabel("C").optionText(options[2]).question(question).build());
            questionOptions.add(QuestionOption.builder().optionLabel("D").optionText(options[3]).question(question).build());
            
            question.setOptions(questionOptions);
            log.info("Fallback parsing successful for question {}", questionNumber);
            return question;
        }
        
        log.error("Fallback parsing also failed for question {}", questionNumber);
        return null;
    }
    
    private String extractValue(String text, String startMarker, String endMarker) {
        if (text == null || startMarker == null) return null;
        
        int start = text.indexOf(startMarker);
        if (start == -1) {
            log.debug("Start marker '{}' not found in text: {}", startMarker, text.substring(0, Math.min(100, text.length())));
            return null;
        }
        
        start += startMarker.length();
        int end;
        
        if (endMarker.isEmpty()) {
            end = text.length();
        } else {
            end = text.indexOf(endMarker, start);
            if (end == -1) {
                end = text.length();
            }
        }
        
        String result = text.substring(start, end).trim();
        log.debug("Extracted value for '{}': '{}'", startMarker, result);
        return result;
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
     * Extract the actual content from OpenAI's JSON response
     */
    private String extractContentFromJsonResponse(String jsonResponse) {
        try {
            log.debug("Extracting content from JSON response, length: {}", jsonResponse.length());
            
            // Look for the content field in the JSON response
            int contentStart = jsonResponse.indexOf("\"content\":");
            if (contentStart == -1) {
                log.warn("No content field found in JSON response, returning raw response");
                return jsonResponse;
            }
            
            // Find the start of the content value (after the opening quote)
            int valueStart = jsonResponse.indexOf("\"", contentStart + "\"content\":".length());
            if (valueStart == -1) {
                log.warn("Malformed content field, returning raw response");
                return jsonResponse;
            }
            valueStart++; // Move past the opening quote
            
            // Find the end of the content value (before the closing quote, accounting for escaped quotes)
            int valueEnd = valueStart;
            boolean escaped = false;
            while (valueEnd < jsonResponse.length()) {
                char c = jsonResponse.charAt(valueEnd);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break; // Found the closing quote
                }
                valueEnd++;
            }
            
            if (valueEnd >= jsonResponse.length()) {
                log.warn("Could not find end of content field, returning raw response");
                return jsonResponse;
            }
            
            String content = jsonResponse.substring(valueStart, valueEnd);
            log.debug("Extracted content length: {}", content.length());
            
            return content;
            
        } catch (Exception e) {
            log.error("Error extracting content from JSON response: {}", e.getMessage(), e);
            log.warn("Falling back to raw response");
            return jsonResponse;
        }
    }
    
    /**
     * Clean up the response text by unescaping common escape sequences
     * that might be returned by the OpenAI API
     */
        private String cleanResponseText(String response) {
        if (response == null) return null;

        // Remove any JSON content that might be mixed in with the response
        String cleaned = response;

        // Look for the start of the actual quiz content (usually starts with TITLE:)
        int titleIndex = cleaned.indexOf("TITLE:");
        if (titleIndex > 0) {
            cleaned = cleaned.substring(titleIndex);
        }

        // Remove any trailing JSON content - look for multiple patterns
        String[] jsonPatterns = {
            "\"refusal\":", "\"annotations\":", "\"logprobs\":", "\"finish_reason\":", 
            "\"usage\":", "\"choices\":", "\"model\":", "\"object\":", "\"created\":"
        };
        
        for (String pattern : jsonPatterns) {
            int jsonStart = cleaned.indexOf(pattern);
            if (jsonStart > 0) {
                cleaned = cleaned.substring(0, jsonStart);
                break; // Stop at first JSON pattern found
            }
        }
        
        // Also remove trailing braces and brackets
        int braceStart = cleaned.lastIndexOf("}");
        if (braceStart > cleaned.length() - 50) { // If brace is near the end
            cleaned = cleaned.substring(0, braceStart);
        }
        
        int bracketStart = cleaned.lastIndexOf("]");
        if (bracketStart > cleaned.length() - 50) { // If bracket is near the end
            cleaned = cleaned.substring(0, bracketStart);
        }

        // Clean up escape sequences
        cleaned = cleaned
                .replace("\\n", "\n")           // Convert escaped newlines to actual newlines
                .replace("\\t", "\t")           // Convert escaped tabs to actual tabs
                .replace("\\\"", "\"")          // Convert escaped quotes to actual quotes
                .replace("\\\\", "\\")          // Convert escaped backslashes to actual backslashes
                .trim();                        // Remove leading/trailing whitespace

        log.debug("Cleaned response text: {}", cleaned.substring(0, Math.min(200, cleaned.length())));

        return cleaned;
    }
    
    /**
     * Clean up the correct answer to extract just the option label (A, B, C, or D)
     * and convert it to the actual option text for proper comparison
     */
    private String cleanCorrectAnswer(String correctAnswerRaw, String optionA, String optionB, String optionC, String optionD) {
        if (correctAnswerRaw == null) {
            log.warn("Correct answer raw is null, using fallback");
            return optionA != null ? optionA.trim() : "A";
        }
        
        // Remove any extra text and extract just the option label
        String cleaned = correctAnswerRaw.trim();
        
        log.debug("Cleaning correct answer: '{}'", cleaned);
        
        // Look for A, B, C, or D in the text and return the corresponding option text
        if (cleaned.contains("A") || cleaned.equalsIgnoreCase("A")) {
            String result = optionA != null ? optionA.trim() : "A";
            log.debug("Correct answer A selected: '{}'", result);
            return result;
        }
        if (cleaned.contains("B") || cleaned.equalsIgnoreCase("B")) {
            String result = optionB != null ? optionB.trim() : "B";
            log.debug("Correct answer B selected: '{}'", result);
            return result;
        }
        if (cleaned.contains("C") || cleaned.equalsIgnoreCase("C")) {
            String result = optionC != null ? optionC.trim() : "C";
            log.debug("Correct answer C selected: '{}'", result);
            return result;
        }
        if (cleaned.contains("D") || cleaned.equalsIgnoreCase("D")) {
            String result = optionD != null ? optionD.trim() : "D";
            log.debug("Correct answer D selected: '{}'", result);
            return result;
        }
        
        // If no clear label found, log warning and use fallback
        log.warn("No clear option label found in '{}', using fallback option A", cleaned);
        return optionA != null ? optionA.trim() : "A";
    }
    
    /**
     * Clean up the explanation text by removing unwanted prefixes and formatting
     */
    private String cleanExplanationText(String explanation) {
        if (explanation == null) return null;
        
        String cleaned = explanation.trim();
        
        // Remove JSON content that might be mixed in with the explanation
        // Look for the start of JSON content and remove everything from there
        int jsonStart = cleaned.indexOf("\"refusal\":");
        if (jsonStart > 0) {
            cleaned = cleaned.substring(0, jsonStart).trim();
        }
        
        // Remove other JSON patterns
        jsonStart = cleaned.indexOf("}, \"logprobs\":");
        if (jsonStart > 0) {
            cleaned = cleaned.substring(0, jsonStart).trim();
        }
        
        // Remove trailing JSON fragments
        cleaned = cleaned.replaceAll(",\\s*\"[^\"]*\":\\s*[^,}]*$", "");
        cleaned = cleaned.replaceAll("\\}\\s*,\\s*\"[^\"]*\".*$", "");
        
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
