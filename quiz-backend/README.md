# AI-Powered Quiz Backend

A Spring Boot 3.x backend application that generates AI-powered quizzes using OpenAI's GPT models. Users can input a topic, and the system will generate 5 multiple-choice questions with explanations.

## Features

- **AI Quiz Generation**: Generate quizzes on any topic using OpenAI GPT models
- **Multiple Choice Questions**: Each quiz contains exactly 5 questions with 4 options (A, B, C, D)
- **Automatic Scoring**: Immediate scoring and feedback after quiz submission
- **Question Explanations**: Detailed explanations for each correct answer
- **User Attempt Tracking**: Store and retrieve quiz attempts and user history
- **RESTful API**: Clean, documented REST endpoints
- **Swagger Documentation**: Interactive API documentation at `/swagger-ui.html`
- **H2 Database**: In-memory database for development (easily switchable to PostgreSQL)

## Technology Stack

- **Java 17+**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **H2 Database** (runtime)
- **Lombok**
- **Spring WebFlux** (for WebClient)
- **SpringDoc OpenAPI** (Swagger)
- **Spring Cache**

## Prerequisites

- Java 17 or higher
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

## Database Schema

The application uses the following entities:

- **Quiz**: Main quiz information
- **Question**: Individual quiz questions
- **QuestionOption**: Multiple choice options (A, B, C, D)
- **QuizAttempt**: User quiz submissions
- **QuestionResponse**: Individual question responses

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

### Logs

Check application logs for detailed error information. Logging is configured at DEBUG level for development.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.
