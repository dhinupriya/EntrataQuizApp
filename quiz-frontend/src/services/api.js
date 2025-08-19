import axios from 'axios';
import config from '../config/config';

// Create axios instance with default config
const api = axios.create({
  baseURL: config.api.baseURL,
  timeout: config.api.timeout,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Quiz API endpoints
export const quizAPI = {
  // Generate a new quiz
  generate: async (topic, description) => {
    try {
      const response = await api.post(config.api.endpoints.quiz.generate, {
        topic,
        description
      });
      return response.data;
    } catch (error) {
      console.error('Error generating quiz:', error);
      throw error;
    }
  },

  // Submit quiz answers (to be implemented when you have the endpoint)
  submit: async (quizId, answers) => {
    try {
      const response = await api.post(config.api.endpoints.quiz.submit, {
        quizId,
        answers
      });
      return response.data;
    } catch (error) {
      console.error('Error submitting quiz:', error);
      throw error;
    }
  }
};

// Transform backend quiz data to frontend format
export const transformQuizData = (backendQuiz) => {
  return {
    id: backendQuiz.id,
    topic: backendQuiz.topic,
    description: backendQuiz.description,
    questions: backendQuiz.questions.map(q => ({
      id: q.id,
      question: q.questionText.trim(),
      options: q.options.map(opt => opt.optionText.trim()),
      questionNumber: q.questionNumber
    }))
  };
};

export default api;
