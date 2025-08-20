# AI-Powered Quiz Backend

A Spring Boot 3.x backend application that generates AI-powered quizzes using OpenAI's GPT models. Users can input a topic, and the system will generate 5 multiple-choice questions with explanations.

## System Architecture & Technical Decisions

This application follows a modern **microservices-inspired architecture** with a **Spring Boot backend** and **React frontend**, designed for scalability and maintainability. The backend employs a **layered architecture** with clear separation of concerns: Controllers handle HTTP requests, Services contain business logic, and JPA Repositories manage data persistence. **Spring Security** provides authentication with Basic Auth, while **H2 in-memory database** ensures rapid development and testing. The system integrates **OpenAI's GPT-4o-mini** for cost-effective quiz generation, enhanced by **Retrieval-Augmented Generation (RAG)** using Wikipedia APIs to improve factual accuracy. Key technical decisions include using **WebClient** for reactive HTTP calls, **JaCoCo** for test coverage monitoring, and **Maven** for dependency management, resulting in a robust, testable, and scalable quiz platform.


## AI Tool Usage & Rationale

**OpenAI GPT-4o-mini** was selected as the primary AI engine for quiz generation due to its optimal balance of cost-effectiveness, response speed, and quality for educational content creation. The model excels at generating structured, multiple-choice questions with accurate explanations while maintaining consistency in formatting. To address potential hallucination issues common in large language models, the system implements **Retrieval-Augmented Generation (RAG)** by integrating **Wikipedia APIs** that fetch relevant, factual content before quiz generation. This hybrid approach combines the creative question-generation capabilities of GPT-4o-mini with verified information from reliable sources, significantly improving factual accuracy for topics like science, history, and technology. The system includes intelligent topic filtering to determine when RAG enhancement would be beneficial, fallback mechanisms for API failures, and comprehensive error handling to ensure reliable quiz generation even when external services are unavailable.


## 📚 **Architecture Documentation**

- **[📊 Complete Architecture Documentation](ARCHITECTURE.md)** - Comprehensive system architecture, AI/LLM integration, and design patterns

## Features

- **AI Quiz Generation**: Generate quizzes on any topic using OpenAI GPT models
- **Multiple Choice Questions**: Each quiz contains exactly 5 questions with 4 options (A, B, C, D)
- **Automatic Scoring**: Immediate scoring and feedback after quiz submission
- **Question Explanations**: Detailed explanations for each correct answer
- **RESTful API**: Clean, documented REST endpoints with CORS support
- **Swagger Documentation**: Interactive API documentation at `/swagger-ui.html`
- **H2 Database**: In-memory database for development (easily switchable to PostgreSQL)
- **Text Cleaning**: Automatic cleaning of AI-generated text (removes escape characters, emojis, unwanted prefixes)

## Technology Stack

- **Java 17+** (recommended for compatibility)
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **H2 Database** (runtime)
- **Lombok**
- **Spring WebFlux** (for WebClient)
- **SpringDoc OpenAPI** (Swagger)
- **Spring Cache**

## Prerequisites

- Java 17 or higher (required for Mockito compatibility)
- Maven 3.6+
- OpenAI API key

## Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd quiz-backend
```

### 2. Set Environment Variables

Set your OpenAI API key as an environment variable:

```bash
export OPENAI_API_KEY=your_openai_api_key_here
```

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Configuration

The application configuration is in `src/main/resources/application.yml`:

```yaml
openai:
  api-key: ${OPENAI_API_KEY:}
  model: gpt-4o-mini
  base-url: https://api.openai.com/v1

spring:
  datasource:
    url: jdbc:h2:mem:quizdb
    username: sa
    password: password
  
  h2:
    console:
      enabled: true
      path: /h2-console
```

## API Endpoints

### Quiz Management

#### Generate Quiz
```http
POST /api/quizzes/generate
Content-Type: application/json

{
  "topic": "JavaScript Fundamentals",
  "description": "Basic concepts of JavaScript programming"
}
```

#### Get Quiz by ID
```http
GET /api/quizzes/{id}
```

#### Get All Quizzes
```http
GET /api/quizzes
```

#### Search Quizzes by Topic
```http
GET /api/quizzes/search?topic=JavaScript
```

#### Delete Quiz
```http
DELETE /api/quizzes/{id}
```

### Quiz Submissions

#### Submit Quiz Answers
```http
POST /api/quiz-submissions/submit
Content-Type: application/json

{
  "quizId": 1,
  "userName": "john_doe",
  "answers": [
    {
      "questionId": 1,
      "selectedAnswer": "A"
    },
    {
      "questionId": 2,
      "selectedAnswer": "C"
    }
  ]
}
```

#### Get User Quiz History
```http
GET /api/quiz-submissions/user/{userName}/history
```

#### Get Quiz Attempts
```http
GET /api/quiz-submissions/quiz/{quizId}/attempts
```

## CORS Configuration

The application supports Cross-Origin Resource Sharing (CORS) for frontend integration:

- **QuizController**: Has `@CrossOrigin(origins = "*")` for quiz management endpoints
- **QuizSubmissionController**: No CORS restrictions (configure as needed for your frontend)

## Database Schema

The application uses the following entities:

- **Quiz**: Main quiz information
- **Question**: Individual quiz questions with correct answer labels (A, B, C, D)
- **QuestionOption**: Multiple choice options (A, B, C, D)
- **QuizAttempt**: User quiz submissions
- **QuestionResponse**: Individual question responses with feedback

## Text Processing Features

The application automatically cleans AI-generated content:

- **Escape Characters**: Converts `\n`, `\t`, `\"`, `\\` to proper characters
- **Emoji Removal**: Removes emojis and special symbols from explanations and feedback
- **Prefix Cleaning**: Removes unwanted prefixes like "Explanation:" from text
- **Answer Normalization**: Stores correct answers as option labels (A, B, C, D) for proper scoring

## Usage Examples

### 1. Generate a Quiz

```bash
curl -X POST http://localhost:8080/api/quizzes/generate \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Python Programming",
    "description": "Basic Python concepts and syntax"
  }'
```

### 2. Submit Quiz Answers

```bash
curl -X POST http://localhost:8080/api/quiz-submissions/submit \
  -H "Content-Type: application/json" \
  -d '{
    "quizId": 1,
    "userName": "alice",
    "answers": [
      {"questionId": 1, "selectedAnswer": "A"},
      {"questionId": 2, "selectedAnswer": "B"},
      {"questionId": 3, "selectedAnswer": "C"},
      {"questionId": 4, "selectedAnswer": "D"},
      {"questionId": 5, "selectedAnswer": "A"}
    ]
  }'
```

## Development

### Project Structure

```
src/main/java/com/entrata/quiz/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/            # Data Transfer Objects
├── entity/         # JPA entities
├── exception/      # Exception handling
├── repository/     # Data access layer
├── service/        # Business logic
└── QuizApplication.java
```

### Adding New Features

1. **New Entity**: Create entity class in `entity/` package
2. **Repository**: Create repository interface in `repository/` package
3. **Service**: Implement business logic in `service/` package
4. **Controller**: Create REST endpoints in `controller/` package
5. **DTOs**: Create request/response DTOs in `dto/` package

## Testing

Run the test suite:

```bash
mvn test
```

**Note**: The project includes working entity tests and basic Spring context tests. Mockito-based service tests have been removed due to Java version compatibility issues.

### Test Coverage

- **Entity Tests**: JPA entity validation and relationships
- **Spring Context**: Basic application context loading
- **Integration**: Database operations and service layer functionality

## Database Access

- **H2 Console**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:quizdb`
- **Username**: `sa`
- **Password**: `password`

## Swagger Documentation

Access the interactive API documentation at:
`http://localhost:8080/swagger-ui.html`

## Production Deployment

### Switching to PostgreSQL

1. Update `pom.xml`:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. Update `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/quizdb
    driver-class-name: org.postgresql.Driver
    username: your_username
    password: your_password
```

### Environment Variables

Set these in production:
- `OPENAI_API_KEY`: Your OpenAI API key
- `SPRING_PROFILES_ACTIVE`: Set to `prod`
- Database connection details

## Troubleshooting

### Common Issues

1. **OpenAI API Key**: Ensure `OPENAI_API_KEY` environment variable is set
2. **Port Conflicts**: Change port in `application.yml` if 8080 is occupied
3. **Memory Issues**: Increase JVM heap size for large quiz generation
4. **Java Version**: Use Java 17+ for optimal compatibility
5. **CORS Issues**: Frontend requests may need proper CORS configuration

### Recent Fixes

- **Text Formatting**: Fixed display of `\n` characters and escape sequences
- **Answer Mapping**: Corrected scoring logic for proper answer comparison
- **Explanation Display**: Cleaned up unwanted prefixes and emojis
- **CORS Support**: Added cross-origin support for frontend integration

### Logs

Check application logs for detailed error information. Logging is configured at DEBUG level for development.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request