package com.entrata.quiz.config;

import com.entrata.quiz.entity.User;
import com.entrata.quiz.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")  // Don't run in test environment
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final UserService userService;
    
    @Override
    public void run(String... args) throws Exception {
        initializeDefaultUsers();
    }
    
    private void initializeDefaultUsers() {
        log.info("Initializing default users...");
        
        // Create default admin user
        if (!userService.existsByUsername("admin")) {
            try {
                userService.createUser("admin", "admin@quiz.com", "admin123", User.Role.ADMIN);
                log.info("Created default admin user: admin/admin123");
            } catch (Exception e) {
                log.error("Failed to create admin user", e);
            }
        }
        
        // Create default regular user
        if (!userService.existsByUsername("user")) {
            try {
                userService.createUser("user", "user@quiz.com", "user123", User.Role.USER);
                log.info("Created default user: user/user123");
            } catch (Exception e) {
                log.error("Failed to create default user", e);
            }
        }
        
        log.info("Default users initialization completed");
    }
}
