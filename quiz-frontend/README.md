# 🎯 Quiz Generator Frontend

A modern, responsive React application for generating and taking AI-powered quizzes on any topic.

## ✨ Features

- **AI-Powered Quiz Generation**: Create quizzes on any topic with just a topic input
- **Interactive Quiz Interface**: Beautiful, responsive design with smooth navigation
- **Real-time Progress Tracking**: Visual progress bar and question navigation
- **Comprehensive Scoring**: Detailed feedback and explanations for each question
- **Modern UI/UX**: Clean, intuitive interface with smooth animations
- **Mobile Responsive**: Works perfectly on all device sizes

## 🚀 Getting Started

### Prerequisites

- Node.js (version 14 or higher)
- npm or yarn
- Backend API running on `http://localhost:8080`

### UI Features

- **Simplified Form**: Single topic input field for faster quiz generation
- **Clean Interface**: No unnecessary fields or buttons
- **Streamlined UX**: Focus on essential functionality
- **Mobile Optimized**: Responsive design for all devices

### Installation

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd quiz-frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start the development server**
   ```bash
   npm start
   ```

4. **Open your browser**
   Navigate to `http://localhost:3000`

## 🔧 Configuration

### Backend API Endpoints

The application expects the following backend endpoints:

1. **Generate Quiz**
   ```
   POST http://localhost:8080/api/quizzes/generate
   Content-Type: application/json
   
   {
     "topic": "JavaScript Fundamentals"
   }
   ```

2. **Submit Answers** (to be implemented)
   ```
   POST http://localhost:8080/api/quizzes/submit
   Content-Type: application/json
   
   {
     "quizId": "123",
     "answers": {
       "1": 2,
       "2": 0,
       "3": 1
     }
   }
   ```

### Environment Variables

Create a `.env` file in the root directory:

```env
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_API_TIMEOUT=10000
```

**Note**: Copy `env.example` to `.env` and modify the values as needed.

## 📁 Project Structure

```
src/
├── components/          # React components
│   ├── QuizForm.js     # Quiz generation form
│   ├── QuizDisplay.js  # Quiz interface
│   ├── ScoreDisplay.js # Results and feedback
│   └── *.css          # Component-specific styles
├── services/           # API services
│   └── api.js         # Backend API integration
├── config/             # Configuration files
│   └── config.js      # Environment and app settings
├── App.js              # Main application component
├── App.css             # Application styles
├── index.js            # Application entry point
└── index.css           # Global styles
```

## 🎨 Design Features

- **Simplified Interface**: Clean, single-input form for faster quiz generation
- **Gradient Backgrounds**: Modern gradient color schemes
- **Card-based Layout**: Clean, organized information display
- **Smooth Animations**: Hover effects and transitions
- **Progress Indicators**: Visual feedback for quiz progress
- **Responsive Design**: Mobile-first approach with breakpoints
- **Accessibility**: Proper ARIA labels and keyboard navigation

## 🔄 State Management

The application uses React's built-in state management with the following main states:

- `currentView`: Controls which component to display
- `quizData`: Stores the generated quiz information
- `userAnswers`: Tracks user's selected answers
- `scoreData`: Contains quiz results and feedback

## 🚀 Available Scripts

- `npm start` - Start development server
- `npm build` - Build for production
- `npm test` - Run test suite
- `npm eject` - Eject from Create React App

## 🛠️ Technologies Used

- **React 18** - Modern React with hooks
- **CSS3** - Custom styling with modern features
- **Axios** - HTTP client for API calls
- **Create React App** - Development environment

## 📱 Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

If you encounter any issues or have questions:

1. Check the browser console for error messages
2. Verify your backend API is running and accessible
3. Ensure all dependencies are properly installed
4. Check the network tab for API call failures
5. Use the simplified form - only topic input is required
6. Backend must be running on the configured port for real quiz generation

## 🔮 Future Enhancements

- [ ] User authentication and quiz history
- [ ] Quiz sharing and social features
- [ ] Advanced quiz types (matching, fill-in-the-blank)
- [ ] Quiz templates and categories
- [ ] Performance analytics and insights
- [ ] Offline support with service workers
- [ ] Quiz difficulty levels
- [ ] Time-based quizzes
- [ ] Quiz export functionality
