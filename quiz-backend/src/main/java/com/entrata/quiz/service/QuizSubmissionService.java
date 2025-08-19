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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizSubmissionService {
    
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    
    @Transactional
    public QuizResultResponse submitQuiz(QuizSubmissionRequest request) {
        log.info("Processing quiz submission for quiz ID: {} by user: {}", 
                request.getQuizId(), request.getUserName());
        
        // Fetch the quiz
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found with ID: " + request.getQuizId()));
        
        // Process answers and calculate score
        List<QuestionResponse> questionResponses = new ArrayList<>();
        int score = 0;
        
        for (QuizSubmissionRequest.QuestionAnswer answer : request.getAnswers()) {
            Question question = quiz.getQuestions().stream()
                    .filter(q -> q.getId().equals(answer.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Question not found with ID: " + answer.getQuestionId()));
            
            boolean isCorrect = answer.getSelectedAnswer().equals(question.getCorrectAnswer());
            if (isCorrect) {
                score++;
            }
            
            // Create question response
            QuestionResponse questionResponse = QuestionResponse.builder()
                    .question(question)
                    .selectedAnswer(answer.getSelectedAnswer())
                    .isCorrect(isCorrect)
                    .feedback(generateFeedback(question, answer.getSelectedAnswer()))
                    .build();
            
            questionResponses.add(questionResponse);
        }
        
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
        
        return buildQuizResultResponse(savedAttempt, quiz);
    }
    
    private String generateFeedback(Question question, String selectedAnswer) {
        if (selectedAnswer.equals(question.getCorrectAnswer())) {
            return "Correct! " + question.getExplanation();
        } else {
            return String.format("Incorrect. You selected '%s', but the correct answer is '%s'. %s", 
                    selectedAnswer, question.getCorrectAnswer(), question.getExplanation());
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
