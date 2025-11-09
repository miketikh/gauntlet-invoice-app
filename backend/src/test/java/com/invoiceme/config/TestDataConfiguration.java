package com.invoiceme.config;

import com.invoiceme.auth.domain.User;
import com.invoiceme.auth.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test data configuration for integration tests
 * Loads test users into the H2 in-memory database
 */
@TestConfiguration
@Profile("test")
@RequiredArgsConstructor
public class TestDataConfiguration {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner loadTestData() {
        return args -> {
            // Only load if database is empty
            if (userRepository.count() == 0) {
                // Create test admin user
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .build();

                userRepository.save(admin);

                // Create another test user
                User testUser = User.builder()
                        .username("testuser")
                        .password(passwordEncoder.encode("test123"))
                        .build();

                userRepository.save(testUser);

                System.out.println("Test data loaded successfully");
            }
        };
    }
}
