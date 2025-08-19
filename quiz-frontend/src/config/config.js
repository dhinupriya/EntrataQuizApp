// Configuration file for environment-specific settings
const config = {
  // Backend API configuration
  api: {
    baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080',
    timeout: process.env.REACT_APP_API_TIMEOUT || 10000,
    endpoints: {
      quiz: {
        generate: '/api/quizzes/generate',
        submit: '/api/quizzes/submit'
      }
    }
  },
  
  // App configuration
  app: {
    name: 'Quiz Generator',
    version: '1.0.0',
    maxQuestions: 50,
    minQuestions: 3
  }
};

export default config;
