# 🛡️ Quiz Application Security Guide

## Overview

The Quiz Application now includes **Basic Authentication** using Spring Security. This provides secure access to all quiz endpoints while maintaining a simple authentication mechanism.

## 🔐 Security Features Implemented

### ✅ **Authentication & Authorization**
- **Basic Authentication** with username/password
- **Role-based Access Control** (USER, ADMIN)
- **JWT-ready architecture** (can be upgraded later)

### ✅ **Security Hardening**
- **CORS Protection** - Restricted to specific frontend domains
- **CSRF Protection** - Disabled for stateless API
- **H2 Console** - Disabled in production
- **Security Headers** - Automatic security headers
- **Input Validation** - Bean validation on all endpoints

### ✅ **Default Users**
The application creates default users on startup:

| Username | Password | Role  | Email           |
|----------|----------|-------|-----------------|
| `admin`  | `admin123` | ADMIN | admin@quiz.com  |
| `user`   | `user123`  | USER  | user@quiz.com   |

## 🌐 API Endpoints

### **Public Endpoints** (No Authentication Required)
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user  
- `GET /api/auth/me` - Get current user info
- `POST /api/auth/logout` - Logout user
- `GET /swagger-ui/**` - API Documentation
- `GET /actuator/health` - Health check

### **Authenticated Endpoints** (Requires Login)
- `GET /api/quizzes/**` - All quiz management endpoints
- `POST /api/quizzes/**` - Quiz generation and management
- `GET /api/quiz-submissions/**` - Quiz submission endpoints
- `POST /api/quiz-submissions/**` - Submit quiz answers

### **Admin-Only Endpoints**
- `DELETE /api/quizzes/**` - Delete quizzes
- `GET /api/users/**` - User management

## 🚀 How to Use Authentication

### **1. Register a New User**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com", 
    "password": "password123"
  }'
```

### **2. Login (Basic Auth)**
For all authenticated endpoints, include Basic Auth headers:

```bash
# Using curl with Basic Auth
curl -X GET http://localhost:8080/api/quizzes \
  -u "user:user123"

# Or with explicit header
curl -X GET http://localhost:8080/api/quizzes \
  -H "Authorization: Basic dXNlcjp1c2VyMTIz"
```

### **3. Generate a Quiz (Authenticated)**
```bash
curl -X POST http://localhost:8080/api/quizzes/generate \
  -u "user:user123" \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Java Programming",
    "difficulty": "MEDIUM"
  }'
```

### **4. Submit Quiz Answers (Authenticated)**
```bash
curl -X POST http://localhost:8080/api/quiz-submissions/submit \
  -u "user:user123" \
  -H "Content-Type: application/json" \
  -d '{
    "quizId": 1,
    "userName": "user",
    "answers": [
      {"questionId": 1, "selectedAnswer": "A"},
      {"questionId": 2, "selectedAnswer": "B"}
    ]
  }'
```

## 🔧 Frontend Integration

### **JavaScript/React Example**
```javascript
// Set up Basic Auth for all API calls
const username = 'user';
const password = 'user123';
const basicAuth = 'Basic ' + btoa(username + ':' + password);

// Configure axios with authentication
axios.defaults.headers.common['Authorization'] = basicAuth;

// Or for individual requests
const response = await axios.get('/api/quizzes', {
  auth: {
    username: 'user',
    password: 'user123'
  }
});
```

## 🛡️ Security Best Practices

### **For Development**
1. Use the default users for testing
2. Always test with authentication enabled
3. Verify CORS settings match your frontend domain

### **For Production**
1. **Change default passwords** immediately
2. **Use HTTPS** - Never send credentials over HTTP
3. **Update CORS origins** to your production domain
4. **Consider JWT** for better scalability
5. **Enable audit logging** for security events

## 🔄 Upgrading to JWT (Future)

The current architecture is designed to easily upgrade to JWT tokens:

1. Add JWT dependencies
2. Create JWT utility classes  
3. Modify authentication endpoints to return tokens
4. Update security config to validate JWT tokens
5. Keep Basic Auth as fallback for admin access

## 🧪 Testing

### **Integration Tests**
- Tests use `@ActiveProfiles("test")` to bypass security
- Security is automatically disabled in test environment
- All existing tests continue to work

### **Manual Testing**
```bash
# Test authentication
curl -X GET http://localhost:8080/api/quizzes

# Should return 401 Unauthorized

curl -X GET http://localhost:8080/api/quizzes -u "user:user123"

# Should return quiz data
```

## 🚨 Security Considerations

### **Current Limitations**
1. **Basic Auth** - Credentials sent with every request
2. **No session timeout** - Stateless but no automatic logout
3. **No password complexity** - Simple validation only
4. **No rate limiting** - Could be added later

### **Mitigations Implemented**
1. **CORS restrictions** - Prevents cross-origin attacks
2. **Input validation** - Prevents malicious data
3. **Error handling** - No information disclosure
4. **Secure headers** - Browser security features enabled

---

## 📞 Support

If you encounter any authentication issues:
1. Check your username/password
2. Verify the endpoint requires authentication
3. Ensure you're using the correct HTTP method
4. Check the server logs for detailed error messages
