package com.entrata.quiz.controller;

import com.entrata.quiz.entity.User;
import com.entrata.quiz.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {
    
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for username: {}", request.getUsername());
        
        try {
            User user = userService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                User.Role.USER
            );
            
            return ResponseEntity.ok(new AuthResponse(
                "User registered successfully",
                user.getUsername(),
                user.getRole().name()
            ));
            
        } catch (RuntimeException e) {
            log.error("Registration failed for username: {}", request.getUsername(), e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user with username and password")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for username: {}", request.getUsername());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();
            
            return ResponseEntity.ok(new AuthResponse(
                "Login successful",
                user.getUsername(),
                user.getRole().name()
            ));
            
        } catch (Exception e) {
            log.error("Login failed for username: {}", request.getUsername(), e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid username or password"));
        }
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Not authenticated"));
        }
        
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new UserInfoResponse(
            user.getUsername(),
            user.getEmail(),
            user.getRole().name(),
            user.getCreatedAt()
        ));
    }
    
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Clear authentication session")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new AuthResponse("Logout successful", null, null));
    }
    
    // DTOs
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;
        
        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        private String email;
        
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        private String password;
    }
    
    @Data
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;
        
        @NotBlank(message = "Password is required")
        private String password;
    }
    
    @Data
    public static class AuthResponse {
        private String message;
        private String username;
        private String role;
        private LocalDateTime timestamp;
        
        public AuthResponse(String message, String username, String role) {
            this.message = message;
            this.username = username;
            this.role = role;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    @Data
    public static class UserInfoResponse {
        private String username;
        private String email;
        private String role;
        private LocalDateTime createdAt;
        private LocalDateTime timestamp;
        
        public UserInfoResponse(String username, String email, String role, LocalDateTime createdAt) {
            this.username = username;
            this.email = email;
            this.role = role;
            this.createdAt = createdAt;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    @Data
    public static class ErrorResponse {
        private String error;
        private LocalDateTime timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = LocalDateTime.now();
        }
    }
}
