import React, { useState } from 'react';
import './QuizDisplay.css';

const QuizDisplay = ({ quiz, onSubmit, onBack }) => {
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [selectedAnswers, setSelectedAnswers] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const currentQuestion = quiz.questions[currentQuestionIndex];
  const totalQuestions = quiz.questions.length;
  const progress = ((currentQuestionIndex + 1) / totalQuestions) * 100;

  const handleAnswerSelect = (questionId, answerIndex) => {
    setSelectedAnswers(prev => ({
      ...prev,
      [questionId]: answerIndex
    }));
  };

  const handleNext = () => {
    if (currentQuestionIndex < totalQuestions - 1) {
      setCurrentQuestionIndex(prev => prev + 1);
    }
  };

  const handlePrevious = () => {
    if (currentQuestionIndex > 0) {
      setCurrentQuestionIndex(prev => prev - 1);
    }
  };

  const handleSubmit = async () => {
    // Check if all questions are answered
    const unansweredQuestions = quiz.questions.filter(
      q => selectedAnswers[q.id] === undefined
    );

    if (unansweredQuestions.length > 0) {
      alert(`Please answer all ${unansweredQuestions.length} remaining questions before submitting.`);
      return;
    }

    setIsSubmitting(true);
    
    try {
      // Here you would call the second endpoint to submit answers
      // For now, we'll simulate the API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      onSubmit(selectedAnswers);
    } catch (error) {
      console.error('Error submitting quiz:', error);
      alert('Failed to submit quiz. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getProgressColor = () => {
    if (progress <= 33) return '#dc3545';
    if (progress <= 66) return '#ffc107';
    return '#28a745';
  };

  return (
    <div className="card quiz-display">
      <div className="quiz-header">
        <div>
          <h2 className="quiz-title">{quiz.topic}</h2>
          <p className="quiz-description">{quiz.description}</p>
        </div>
        <div className="quiz-progress">
          Question {currentQuestionIndex + 1} of {totalQuestions}
        </div>
      </div>

      <div className="progress-bar">
        <div 
          className="progress-fill" 
          style={{ 
            width: `${progress}%`,
            backgroundColor: getProgressColor()
          }}
        ></div>
      </div>

      <div className="quiz-question">
        <h3 className="question-text">
          {currentQuestion.question}
        </h3>
        
        <ul className="options-list">
          {currentQuestion.options.map((option, index) => (
            <li 
              key={index}
              className={`option-item ${
                selectedAnswers[currentQuestion.id] === index ? 'selected' : ''
              }`}
              onClick={() => handleAnswerSelect(currentQuestion.id, index)}
            >
              <input
                type="radio"
                name={`question-${currentQuestion.id}`}
                value={index}
                checked={selectedAnswers[currentQuestion.id] === index}
                onChange={() => handleAnswerSelect(currentQuestion.id, index)}
                className="option-radio"
              />
              <span className="option-text">{option}</span>
            </li>
          ))}
        </ul>
      </div>

      <div className="quiz-actions">
        <button
          className="btn btn-secondary"
          onClick={onBack}
          disabled={isSubmitting}
        >
          ← Back to Form
        </button>
        
        <button
          className="btn btn-secondary"
          onClick={handlePrevious}
          disabled={currentQuestionIndex === 0 || isSubmitting}
        >
          ← Previous
        </button>
        
        {currentQuestionIndex < totalQuestions - 1 ? (
          <button
            className="btn btn-secondary"
            onClick={handleNext}
            disabled={isSubmitting}
          >
            Next →
          </button>
        ) : (
          <button
            className="btn"
            onClick={handleSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <>
                <span className="spinner"></span>
                Submitting...
              </>
            ) : (
              '🎯 Submit Quiz'
            )}
          </button>
        )}
      </div>

      <div className="question-navigation">
        {quiz.questions.map((_, index) => (
          <button
            key={index}
            className={`nav-dot ${
              index === currentQuestionIndex ? 'active' : ''
            } ${
              selectedAnswers[quiz.questions[index].id] !== undefined ? 'answered' : ''
            }`}
            onClick={() => setCurrentQuestionIndex(index)}
            disabled={isSubmitting}
          >
            {index + 1}
          </button>
        ))}
      </div>
    </div>
  );
};

export default QuizDisplay;
