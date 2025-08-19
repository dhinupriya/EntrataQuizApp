package com.entrata.quiz.service;

import com.entrata.quiz.dto.QuizGenerationRequest;
import com.entrata.quiz.dto.QuizResponse;
import com.entrata.quiz.entity.Question;
import com.entrata.quiz.entity.QuestionOption;
import com.entrata.quiz.entity.Quiz;
import com.entrata.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {
    
    private final QuizRepository quizRepository;
    private final OpenAiService openAiService;
    
    @Transactional
    public QuizResponse generateAndSaveQuiz(QuizGenerationRequest request) {
        log.info("Generating quiz for topic: {}", request.getTopic());
        
        // Generate quiz using OpenAI
        Quiz quiz = openAiService.generateQuiz(request);
        
        // Save quiz to database
        Quiz savedQuiz = quizRepository.save(quiz);
        
        log.info("Quiz generated and saved with ID: {}", savedQuiz.getId());
        
        return mapToQuizResponse(savedQuiz);
    }
    
    @Cacheable(value = "quizzes", key = "#id")
    public QuizResponse getQuizById(Long id) {
        log.info("Fetching quiz with ID: {}", id);
        
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz not found with ID: " + id));
        
        return mapToQuizResponse(quiz);
    }
    
    public List<QuizResponse> getAllQuizzes() {
        log.info("Fetching all quizzes");
        
        List<Quiz> quizzes = quizRepository.findAllByOrderByCreatedAtDesc();
        
        return quizzes.stream()
                .map(this::mapToQuizResponse)
                .collect(Collectors.toList());
    }
    
    public List<QuizResponse> searchQuizzesByTopic(String topic) {
        log.info("Searching quizzes by topic: {}", topic);
        
        List<Quiz> quizzes = quizRepository.findByTopicContainingIgnoreCase(topic);
        
        return quizzes.stream()
                .map(this::mapToQuizResponse)
                .collect(Collectors.toList());
    }
    
    @CacheEvict(value = "quizzes", key = "#id")
    public void deleteQuiz(Long id) {
        log.info("Deleting quiz with ID: {}", id);
        
        if (!quizRepository.existsById(id)) {
            throw new RuntimeException("Quiz not found with ID: " + id);
        }
        
        quizRepository.deleteById(id);
    }
    
    private QuizResponse mapToQuizResponse(Quiz quiz) {
        return QuizResponse.builder()
                .id(quiz.getId())
                .topic(quiz.getTopic())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .createdAt(quiz.getCreatedAt())
                .questions(quiz.getQuestions().stream()
                        .map(this::mapToQuestionResponse)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private QuizResponse.QuestionResponse mapToQuestionResponse(Question question) {
        return QuizResponse.QuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionNumber(question.getQuestionNumber())
                .options(question.getOptions().stream()
                        .map(this::mapToQuestionOptionResponse)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private QuizResponse.QuestionOptionResponse mapToQuestionOptionResponse(QuestionOption option) {
        return QuizResponse.QuestionOptionResponse.builder()
                .optionLabel(option.getOptionLabel())
                .optionText(option.getOptionText())
                .build();
    }
}
