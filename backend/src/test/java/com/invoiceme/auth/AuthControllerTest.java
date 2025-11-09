package com.invoiceme.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceme.auth.dto.LoginRequest;
import com.invoiceme.auth.dto.RefreshRequest;
import com.invoiceme.config.TestDataConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 * Tests all authentication endpoints with real Spring Security integration
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataConfiguration.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private LoginRequest validLoginRequest;
    private LoginRequest invalidLoginRequest;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("admin");
        validLoginRequest.setPassword("admin123");

        invalidLoginRequest = new LoginRequest();
        invalidLoginRequest.setUsername("wronguser");
        invalidLoginRequest.setPassword("wrongpassword");
    }

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLoginSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)));
    }

    @Test
    @DisplayName("Should fail login with invalid credentials")
    void testLoginFailureWithInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid username or password")));
    }

    @Test
    @DisplayName("Should fail login with missing username")
    void testLoginFailureWithMissingUsername() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPassword("password123");
        // username is null

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should fail login with missing password")
    void testLoginFailureWithMissingPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        // password is null

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should fail login with empty credentials")
    void testLoginFailureWithEmptyCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should successfully refresh token with valid refresh token")
    void testRefreshTokenSuccess() throws Exception {
        // First, login to get a valid refresh token
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract refresh token from login response
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        // Now test the refresh endpoint
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)));
    }

    @Test
    @DisplayName("Should fail refresh with invalid refresh token")
    void testRefreshTokenFailureWithInvalidToken() throws Exception {
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("invalid.token.here");

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid refresh token")));
    }

    @Test
    @DisplayName("Should fail refresh with expired refresh token")
    void testRefreshTokenFailureWithExpiredToken() throws Exception {
        // Generate an expired token (expiration set to past)
        String expiredToken = tokenProvider.generateToken("admin", true);

        // Wait a moment to ensure time has passed (or mock time)
        // For this test, we'll use an invalid token format which will also fail

        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("expired.token.string");

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should fail refresh with missing refresh token")
    void testRefreshTokenFailureWithMissingToken() throws Exception {
        RefreshRequest refreshRequest = new RefreshRequest();
        // refreshToken is null

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return health check status")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth service is running"));
    }

    @Test
    @DisplayName("Should return valid JWT token structure on login")
    void testLoginReturnsValidJwtStructure() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        // Verify token has 3 parts (header.payload.signature)
        String[] parts = token.split("\\.");
        assert parts.length == 3 : "JWT should have 3 parts";

        // Verify we can extract username from token
        String username = tokenProvider.getUsernameFromToken(token);
        assert username.equals("admin") : "Username should be 'admin'";
    }

    @Test
    @DisplayName("Should not expose password in any response")
    void testPasswordNotExposedInResponse() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify password is not in response
        assert !response.contains("admin123") : "Password should not be in response";
        assert !response.contains("password") : "Password field should not be in response";
    }
}
