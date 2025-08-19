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

// Add request interceptor to include auth headers from global axios defaults
api.interceptors.request.use(
  (config) => {
    // Copy authorization header from global axios defaults if it exists
    if (axios.defaults.headers.common['Authorization']) {
      config.headers['Authorization'] = axios.defaults.headers.common['Authorization'];
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle authentication errors
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      // Clear stored credentials on 401 error
      localStorage.removeItem('quiz_auth_credentials');
      delete axios.defaults.headers.common['Authorization'];
      
      // Redirect to login if not already on login page
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

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

  // Submit quiz answers
  submit: async (quizId, answers, userName = "Anonymous User") => {
    try {
      const response = await api.post(config.api.endpoints.quiz.submit, {
        quizId,
        userName,
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
