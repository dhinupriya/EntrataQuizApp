import React, { useState } from 'react';
import { quizAPI, transformQuizData } from '../services/api';
import './QuizForm.css';

const QuizForm = ({ onQuizGenerated, isLoading, setIsLoading }) => {
  const [formData, setFormData] = useState({
    topic: ''
  });
  const [errors, setErrors] = useState({});

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.topic.trim()) {
      newErrors.topic = 'Topic is required';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setIsLoading(true);
    
    try {
      const quizData = await quizAPI.generate(
        formData.topic.trim()
      );

      // Transform the backend response to match our frontend structure
      const transformedQuiz = transformQuizData(quizData);

      onQuizGenerated(transformedQuiz);
    } catch (error) {
      console.error('Error generating quiz:', error);
      
      if (error.code === 'ERR_NETWORK' || error.message.includes('Network Error')) {
        alert('Cannot connect to backend server. Please ensure your backend is running on http://localhost:8080 or use Demo Mode to test the frontend.');
      } else {
        alert('Failed to generate quiz. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };



  return (
    <div className="card quiz-form">
      <h2>🎯 Generate Quiz</h2>
      
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="topic" className="form-label">
            Quiz Topic *
          </label>
          <input
            type="text"
            id="topic"
            name="topic"
            className={`form-input ${errors.topic ? 'error' : ''}`}
            placeholder="e.g., JavaScript Fundamentals, Python Basics, World History, Math, Science"
            value={formData.topic}
            onChange={handleInputChange}
            disabled={isLoading}
          />
          {errors.topic && <span className="error-message">{errors.topic}</span>}
        </div>



        <div className="form-actions">
          <button
            type="submit"
            className="btn"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <span className="spinner"></span>
                Generating Quiz...
              </>
            ) : (
              '🚀 Generate Quiz'
            )}
          </button>
        </div>
      </form>

      {isLoading && (
        <div className="loading">
          <div className="spinner"></div>
          <p>AI is crafting your perfect quiz...</p>
        </div>
      )}
    </div>
  );
};

export default QuizForm;
