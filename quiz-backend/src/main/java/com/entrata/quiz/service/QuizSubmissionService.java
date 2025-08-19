package com.entrata.quiz.service;

import com.entrata.quiz.dto.QuizResultResponse;
import com.entrata.quiz.dto.QuizSubmissionRequest;
import com.entrata.quiz.entity.*;
import com.entrata.quiz.repository.QuizAttemptRepository;
import com.entrata.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizSubmissionService {
    
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    
    @Transactional
    public QuizAttempt submitQuizAndReturnAttempt(QuizSubmissionRequest request) {
        log.info("Processing quiz submission for quiz ID: {} by user: {}", 
                request.getQuizId(), request.getUserName());
        
        // Fetch the quiz
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found with ID: " + request.getQuizId()));
        
        log.info("Quiz found with {} questions", quiz.getQuestions().size());
        
        // Process answers and calculate score
        List<QuestionResponse> questionResponses = new ArrayList<>();
        int score = 0;
        
        for (QuizSubmissionRequest.QuestionAnswer answer : request.getAnswers()) {
            log.info("Processing answer: questionId={}, selectedAnswer={}", 
                    answer.getQuestionId(), answer.getSelectedAnswer());
            
            Question question = quiz.getQuestions().stream()
                    .filter(q -> q.getId().equals(answer.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Question not found with ID: " + answer.getQuestionId()));
            
            // Get the selected answer text from the option index
            String selectedAnswerText = getOptionTextByIndex(question, answer.getSelectedAnswer());
            log.info("Question: '{}', Selected: '{}', Correct: '{}', Options: {}", 
                    question.getQuestionText(), selectedAnswerText, question.getCorrectAnswer(), 
                    question.getOptions().stream().map(opt -> opt.getOptionText()).collect(Collectors.toList()));
            
            // Compare the actual option text with the correct answer
            boolean isCorrect = selectedAnswerText.equals(question.getCorrectAnswer());
            if (isCorrect) {
                score++;
                log.info("Answer CORRECT - Score incremented to: {}", score);
            } else {
                log.info("Answer INCORRECT - Score remains: {}", score);
            }
            
            // Create question response
            QuestionResponse questionResponse = QuestionResponse.builder()
                    .question(question)
                    .selectedAnswer(selectedAnswerText)
                    .isCorrect(isCorrect)
                    .feedback(generateFeedback(question, selectedAnswerText))
                    .build();
            
            questionResponses.add(questionResponse);
        }
        
        log.info("Final score calculation: {}/{}", score, quiz.getQuestions().size());
        
        // Create quiz attempt
        QuizAttempt quizAttempt = QuizAttempt.builder()
                .quiz(quiz)
                .userName(request.getUserName())
                .score(score)
                .totalQuestions(quiz.getQuestions().size())
                .responses(questionResponses)
                .build();
        
        // Set the quiz attempt reference in responses
        questionResponses.forEach(response -> response.setQuizAttempt(quizAttempt));
        
        // Save the attempt
        QuizAttempt savedAttempt = quizAttemptRepository.save(quizAttempt);
        
        log.info("Quiz submission processed. Score: {}/{}", score, quiz.getQuestions().size());
        
        return savedAttempt;
    }
    
    @Transactional
    public QuizResultResponse submitQuiz(QuizSubmissionRequest request) {
        QuizAttempt attempt = submitQuizAndReturnAttempt(request);
        return buildQuizResultResponse(attempt, attempt.getQuiz());
    }
    
    private String generateFeedback(Question question, String selectedAnswer) {
        if (selectedAnswer.equals(question.getCorrectAnswer())) {
            return "Correct! " + question.getExplanation();
        } else {
            return String.format("Incorrect. You selected '%s', but the correct answer is '%s'. %s", 
                    selectedAnswer, question.getCorrectAnswer(), question.getExplanation());
        }
    }
    
    /**
     * Helper method to get the option text by index
     * The selectedAnswer comes as a string representing the option index
     */
    private String getOptionTextByIndex(Question question, String selectedAnswer) {
        try {
            int optionIndex = Integer.parseInt(selectedAnswer);
            if (optionIndex < 0 || optionIndex >= question.getOptions().size()) {
                throw new RuntimeException("Invalid option index: " + optionIndex);
            }
            return question.getOptions().get(optionIndex).getOptionText();
        } catch (NumberFormatException e) {
            // If it's not a number, assume it's already the answer text
            return selectedAnswer;
        }
    }
    
    private QuizResultResponse buildQuizResultResponse(QuizAttempt attempt, Quiz quiz) {
        double percentage = (double) attempt.getScore() / attempt.getTotalQuestions() * 100;
        
        return QuizResultResponse.builder()
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .userName(attempt.getUserName())
                .score(attempt.getScore())
                .totalQuestions(attempt.getTotalQuestions())
                .percentage(Math.round(percentage * 100.0) / 100.0)
                .submittedAt(attempt.getSubmittedAt())
                .questionResults(attempt.getResponses().stream()
                        .map(this::mapToQuestionResult)
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * Create a frontend-compatible response format
     */
    public Map<String, Object> buildFrontendResponse(QuizAttempt attempt, Quiz quiz) {
        double percentage = (double) attempt.getScore() / attempt.getTotalQuestions() * 100;
        
        // Create feedback array in the format frontend expects
        List<Map<String, Object>> feedback = attempt.getResponses().stream()
                .map(response -> {
                    Map<String, Object> feedbackItem = new HashMap<>();
                    feedbackItem.put("questionIndex", response.getQuestion().getQuestionNumber() - 1); // Convert to 0-based index
                    feedbackItem.put("correct", response.getIsCorrect());
                    feedbackItem.put("explanation", response.getFeedback());
                    feedbackItem.put("correctAnswer", response.getQuestion().getCorrectAnswer()); // Add correct answer
                    return feedbackItem;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> frontendResponse = new HashMap<>();
        frontendResponse.put("score", attempt.getScore());
        frontendResponse.put("totalQuestions", attempt.getTotalQuestions());
        frontendResponse.put("feedback", feedback);
        
        return frontendResponse;
    }
    
    private QuizResultResponse.QuestionResult mapToQuestionResult(QuestionResponse response) {
        return QuizResultResponse.QuestionResult.builder()
                .questionId(response.getQuestion().getId())
                .questionText(response.getQuestion().getQuestionText())
                .selectedAnswer(response.getSelectedAnswer())
                .correctAnswer(response.getQuestion().getCorrectAnswer())
                .isCorrect(response.getIsCorrect())
                .explanation(response.getQuestion().getExplanation())
                .feedback(response.getFeedback())
                .build();
    }
    
    public List<QuizResultResponse> getUserQuizHistory(String userName) {
        log.info("Fetching quiz history for user: {}", userName);
        
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserNameOrderBySubmittedAtDesc(userName);
        
        return attempts.stream()
                .map(attempt -> buildQuizResultResponse(attempt, attempt.getQuiz()))
                .collect(Collectors.toList());
    }
    
    public List<QuizResultResponse> getQuizAttempts(Long quizId) {
        log.info("Fetching all attempts for quiz ID: {}", quizId);
        
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdOrderBySubmittedAtDesc(quizId);
        
        return attempts.stream()
                .map(attempt -> buildQuizResultResponse(attempt, attempt.getQuiz()))
                .collect(Collectors.toList());
    }
}
