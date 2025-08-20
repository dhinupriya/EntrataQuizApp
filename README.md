# Quiz Application

A full-stack quiz generation and management system that leverages AI to create educational quizzes with retrieval-augmented generation (RAG) for enhanced factual accuracy.

## System Architecture & Technical Decisions

This application follows a modern **microservices-inspired architecture** with a **Spring Boot backend** and **React frontend**, designed for scalability and maintainability. The backend employs a **layered architecture** with clear separation of concerns: Controllers handle HTTP requests, Services contain business logic, and JPA Repositories manage data persistence. **Spring Security** provides authentication with Basic Auth, while **H2 in-memory database** ensures rapid development and testing. The system integrates **OpenAI's GPT-4o-mini** for cost-effective quiz generation, enhanced by **Retrieval-Augmented Generation (RAG)** using Wikipedia APIs to improve factual accuracy. Key technical decisions include using **WebClient** for reactive HTTP calls, **JaCoCo** for test coverage monitoring, and **Maven** for dependency management, resulting in a robust, testable, and scalable quiz platform.

The frontend is built with **React 18** using modern hooks and context patterns for state management, **Axios** for API communication with request/response interceptors for authentication, and **CSS modules** for component styling. The application implements **JWT-like session management** through localStorage and provides a responsive user interface with real-time feedback. Technical decisions prioritized **user experience** with features like demo user fill buttons, loading states, error handling, and automatic authentication token management. The architecture supports easy extension for additional quiz types, user roles, and integration with other AI services.

## AI Tool Usage & Rationale

**OpenAI GPT-4o-mini** was selected as the primary AI engine for quiz generation due to its optimal balance of cost-effectiveness, response speed, and quality for educational content creation. The model excels at generating structured, multiple-choice questions with accurate explanations while maintaining consistency in formatting. To address potential hallucination issues common in large language models, the system implements **Retrieval-Augmented Generation (RAG)** by integrating **Wikipedia APIs** that fetch relevant, factual content before quiz generation. This hybrid approach combines the creative question-generation capabilities of GPT-4o-mini with verified information from reliable sources, significantly improving factual accuracy for topics like science, history, and technology. The system includes intelligent topic filtering to determine when RAG enhancement would be beneficial, fallback mechanisms for API failures, and comprehensive error handling to ensure reliable quiz generation even when external services are unavailable.

## Features

- 🤖 **AI-Powered Quiz Generation** - Create quizzes on any topic using GPT-4o-mini
- 🧠 **Retrieval-Augmented Generation** - Enhanced accuracy with Wikipedia integration
- 🔐 **User Authentication** - Secure login with role-based access control
- 📊 **Real-time Scoring** - Instant feedback with detailed explanations
- ⚡ **Modern Tech Stack** - Spring Boot 3, React 18, Java 17

## Quick Start

### Backend
```bash
cd quiz-backend
mvn spring-boot:run
```

### Frontend
```bash
cd quiz-frontend
npm install
npm start
```

### Default Users
- **Admin**: `admin/admin123`
- **User**: `user/user123`

## API Endpoints

- `POST /api/quiz/generate` - Generate new quiz
- `POST /api/quiz-submission/submit` - Submit quiz answers
- `GET /api/quiz` - List all quizzes
- `POST /api/auth/login` - User authentication

## Tech Stack

**Backend:**
- Spring Boot 3.2.0
- Java 17
- Spring Security
- Spring Data JPA
- H2 Database
- OpenAI API Integration
- Maven

**Frontend:**
- React 18
- Axios
- CSS3
- Context API
- Modern Hooks

**Testing & Quality:**
- JUnit 5
- Mockito
- JaCoCo Coverage
- Integration Tests

## Project Structure

```
EntrataQuizApp/
├── quiz-backend/          # Spring Boot API
│   ├── src/main/java/
│   ├── src/test/java/
│   └── pom.xml
├── quiz-frontend/         # React Application
│   ├── src/
│   ├── public/
│   └── package.json
└── README.md
```

