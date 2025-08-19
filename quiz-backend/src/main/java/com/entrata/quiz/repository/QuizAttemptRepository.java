package com.entrata.quiz.repository;

import com.entrata.quiz.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    
    List<QuizAttempt> findByQuizIdOrderBySubmittedAtDesc(Long quizId);
    
    List<QuizAttempt> findByUserNameOrderBySubmittedAtDesc(String userName);
}
