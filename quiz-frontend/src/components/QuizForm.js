import React, { useState } from 'react';
import { quizAPI, transformQuizData } from '../services/api';
import './QuizForm.css';

const QuizForm = ({ onQuizGenerated, isLoading, setIsLoading }) => {
  const [formData, setFormData] = useState({
    topic: '',
    description: ''
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
    
    if (!formData.description.trim()) {
      newErrors.description = 'Description is required';
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
        formData.topic.trim(),
        formData.description.trim()
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

  const handleDemoMode = () => {
    // Create a demo quiz without calling the backend
    const demoQuiz = {
      id: Date.now(),
      topic: "JavaScript Fundamentals",
      description: "A demo quiz to test the frontend functionality",
      questions: [
        {
          id: 1,
          questionText: "What keyword is used to declare a variable in JavaScript?",
          options: [
            {"optionLabel": "A", "optionText": "var"},
            {"optionLabel": "B", "optionText": "let"},
            {"optionLabel": "C", "optionText": "const"},
            {"optionLabel": "D", "optionText": "All of the above"}
          ],
          questionNumber: 1
        },
        {
          id: 2,
          questionText: "Which of the following is a primitive data type in JavaScript?",
          options: [
            {"optionLabel": "A", "optionText": "Array"},
            {"optionLabel": "B", "optionText": "Object"},
            {"optionLabel": "C", "optionText": "String"},
            {"optionLabel": "D", "optionText": "Function"}
          ],
          questionNumber: 2
        },
        {
          id: 3,
          questionText: "What will console.log(typeof null) output?",
          options: [
            {"optionLabel": "A", "optionText": "null"},
            {"optionLabel": "B", "optionText": "object"},
            {"optionLabel": "C", "optionText": "undefined"},
            {"optionLabel": "D", "optionText": "number"}
          ],
          questionNumber: 3
        },
        {
          id: 4,
          questionText: "Which method is used to parse a JSON string into a JavaScript object?",
          options: [
            {"optionLabel": "A", "optionText": "JSON.parse()"},
            {"optionLabel": "B", "optionText": "JSON.stringify()"},
            {"optionLabel": "C", "optionText": "JSON.convert()"},
            {"optionLabel": "D", "optionText": "JSON.objectify()"}
          ],
          questionNumber: 4
        },
        {
          id: 5,
          questionText: "What is the correct way to create an anonymous function in JavaScript?",
          options: [
            {"optionLabel": "A", "optionText": "function myFunc() {}"},
            {"optionLabel": "B", "optionText": "var myFunc = function() {};"},
            {"optionLabel": "C", "optionText": "function: myFunc() {};"},
            {"optionLabel": "D", "optionText": "myFunc() => {}"}
          ],
          questionNumber: 5
        }
      ]
    };

    // Transform the demo quiz to match our frontend structure
    const transformedQuiz = transformQuizData(demoQuiz);
    onQuizGenerated(transformedQuiz);
  };

  return (
    <div className="card quiz-form">
      <h2>🎯 Generate Your Quiz</h2>
      
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
            placeholder="e.g., JavaScript Fundamentals, Python Basics, World History"
            value={formData.topic}
            onChange={handleInputChange}
            disabled={isLoading}
          />
          {errors.topic && <span className="error-message">{errors.topic}</span>}
        </div>

        <div className="form-group">
          <label htmlFor="description" className="form-label">
            Description *
          </label>
          <textarea
            id="description"
            name="description"
            className={`form-input textarea ${errors.description ? 'error' : ''}`}
            placeholder="Provide a brief description of what you want to learn or test..."
            value={formData.description}
            onChange={handleInputChange}
            disabled={isLoading}
          />
          {errors.description && <span className="error-message">{errors.description}</span>}
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
          
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleDemoMode}
            disabled={isLoading}
          >
            🎮 Demo Mode
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
