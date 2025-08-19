import React, { useState } from 'react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './components/Login';
import Header from './components/Header';
import QuizForm from './components/QuizForm';
import QuizDisplay from './components/QuizDisplay';
import ScoreDisplay from './components/ScoreDisplay';
import { quizAPI } from './services/api';
import './App.css';

// Main App component wrapped with authentication
function AppContent() {
  const { isAuthenticated, isLoading: authLoading, user } = useAuth();
  const [currentView, setCurrentView] = useState('form'); // 'form', 'quiz', 'score'
  const [quizData, setQuizData] = useState(null);
  const [userAnswers, setUserAnswers] = useState({});
  const [scoreData, setScoreData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);

  // Show loading spinner while checking authentication
  if (authLoading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Loading...</p>
        </div>
      </div>
    );
  }

  // Show login page if not authenticated
  if (!isAuthenticated) {
    return <Login />;
  }

  const handleQuizGenerated = (quiz) => {
    console.log('Quiz generated:', quiz);
    console.log('Quiz questions:', quiz.questions);
    setQuizData(quiz);
    setUserAnswers({});
    setCurrentView('quiz');
  };

  const handleQuizSubmitted = async (answers) => {
    setUserAnswers(answers);
    
    try {
      // Transform the frontend answers format to backend format
      // The answers object has question IDs as keys and selected answer indices as values
      const backendAnswers = Object.entries(answers).map(([questionId, selectedAnswerIndex]) => {
        // Find the actual question object to get the real question ID
        const question = quizData.questions.find(q => q.id.toString() === questionId);
        if (!question) {
          console.error(`Question not found for ID: ${questionId}`);
          return null;
        }
        
        return {
          questionId: question.id, // Use the actual question ID from the question object
          selectedAnswer: selectedAnswerIndex.toString() // Convert index to string for backend
        };
      }).filter(Boolean); // Remove any null entries
      
      console.log('Frontend answers:', answers);
      console.log('Backend answers:', backendAnswers);
      
      // Call the backend API to submit answers with username
      const result = await quizAPI.submit(quizData.id, backendAnswers, user?.username || 'Anonymous User');
      setScoreData(result);
      setCurrentView('score');
    } catch (error) {
      console.error('Error submitting quiz:', error);
      alert('Failed to submit quiz. Please try again.');
      
      // Fallback to mock data if API fails
      const mockScore = {
        score: Math.min(Math.floor(Math.random() * quizData.questions.length), quizData.questions.length),
        totalQuestions: quizData.questions.length,
        feedback: quizData.questions.map((q, index) => ({
          questionIndex: index,
          correct: Math.random() > 0.5,
          explanation: `This is feedback for question ${index + 1}`,
          correctAnswer: q.options[Math.floor(Math.random() * q.options.length)] // Random correct answer from options
        }))
      };
      setScoreData(mockScore);
      setCurrentView('score');
    }
  };

  const handleNewQuiz = () => {
    setCurrentView('form');
    setQuizData(null);
    setUserAnswers({});
    setScoreData(null);
  };

  const renderCurrentView = () => {
    switch (currentView) {
      case 'form':
        return (
          <QuizForm 
            onQuizGenerated={handleQuizGenerated}
            isLoading={isLoading}
            setIsLoading={setIsLoading}
          />
        );
      case 'quiz':
        return (
          <QuizDisplay 
            quiz={quizData}
            onSubmit={handleQuizSubmitted}
            onBack={handleNewQuiz}
          />
        );
      case 'score':
        return (
          <ScoreDisplay 
            scoreData={scoreData}
            quiz={quizData}
            userAnswers={userAnswers}
            onNewQuiz={handleNewQuiz}
          />
        );
      default:
        return <QuizForm onQuizGenerated={handleQuizGenerated} />;
    }
  };

  return (
    <div className="App">
      <Header />
      
      <div className="container">
        {renderCurrentView()}
      </div>
    </div>
  );
}

// Main App component with AuthProvider
function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
