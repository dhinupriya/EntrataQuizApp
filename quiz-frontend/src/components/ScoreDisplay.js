import React from 'react';
import './ScoreDisplay.css';

const ScoreDisplay = ({ scoreData, quiz, userAnswers, onNewQuiz }) => {
  const { score, totalQuestions, feedback } = scoreData;
  const percentage = Math.round((score / totalQuestions) * 100);

  const getScoreMessage = () => {
    if (percentage >= 90) return "🎉 Excellent! You're a master of this topic!";
    if (percentage >= 80) return "🌟 Great job! You have a solid understanding!";
    if (percentage >= 70) return "👍 Good work! You're on the right track!";
    if (percentage >= 60) return "📚 Not bad! Keep studying to improve!";
    if (percentage >= 50) return "💪 You're getting there! More practice needed.";
    return "📖 Keep learning! Every mistake is a learning opportunity!";
  };

  const getScoreColor = () => {
    if (percentage >= 80) return '#28a745';
    if (percentage >= 60) return '#ffc107';
    return '#dc3545';
  };

  return (
    <div className="card score-display">
      <div className="score-header">
        <h2>🎯 Quiz Results</h2>
        <p className="score-subtitle">Here's how you performed on "{quiz.topic}"</p>
      </div>

      <div className="score-summary">
        <div className="score-circle" style={{ borderColor: getScoreColor() }}>
          <div className="score-number" style={{ color: getScoreColor() }}>
            {percentage}%
          </div>
          <div className="score-fraction">
            {score}/{totalQuestions}
          </div>
        </div>
        
        <div className="score-message">
          <h3>{getScoreMessage()}</h3>
          <p>You answered {score} out of {totalQuestions} questions correctly.</p>
        </div>
      </div>

      <div className="feedback-section">
        <h3>📝 Question-by-Question Feedback</h3>
        <div className="feedback-grid">
          {feedback.map((item, index) => {
            const question = quiz.questions[item.questionIndex];
            const userAnswer = userAnswers[question.id];
            const isCorrect = item.correct;
            
            return (
              <div 
                key={index} 
                className={`feedback-item ${isCorrect ? 'correct' : 'incorrect'}`}
              >
                <div className="feedback-header">
                  <span className="question-number">Question {item.questionIndex + 1}</span>
                  <span className={`feedback-status ${isCorrect ? 'correct' : 'incorrect'}`}>
                    {isCorrect ? '✅ Correct' : '❌ Incorrect'}
                  </span>
                </div>
                
                <div className="question-content">
                  <p className="question-text">{question.question}</p>
                  
                  <div className="answer-details">
                    <div className="user-answer">
                      <strong>Your answer:</strong> {question.options[userAnswer]}
                    </div>
                    
                    {!isCorrect && (
                      <div className="correct-answer">
                        <strong>Correct answer:</strong> {question.options[question.correctAnswer]}
                      </div>
                    )}
                  </div>
                  
                  {item.explanation && (
                    <div className="explanation">
                      <strong>Explanation:</strong> {item.explanation}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="score-actions">
        <button className="btn" onClick={onNewQuiz}>
          🚀 Generate New Quiz
        </button>
      </div>
    </div>
  );
};

export default ScoreDisplay;
