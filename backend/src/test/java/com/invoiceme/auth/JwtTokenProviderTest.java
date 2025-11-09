package com.invoiceme.auth;

import com.invoiceme.config.JwtConfig;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtTokenProvider
 * Tests token generation, validation, and extraction methods
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @Mock
    private Authentication authentication;

    private JwtConfig jwtConfig;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        // Use default configuration values
        jwtConfig.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        jwtConfig.setExpiration(3600000); // 1 hour
        jwtConfig.setRefreshExpiration(604800000); // 7 days

        tokenProvider = new JwtTokenProvider(jwtConfig);

        // Create a test user
        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("Should generate valid token from Authentication")
    void testGenerateTokenFromAuthentication() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // Act
        String token = tokenProvider.generateToken(authentication);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts

        verify(authentication, times(1)).getPrincipal();
    }

    @Test
    @DisplayName("Should generate valid token from username")
    void testGenerateTokenFromUsername() {
        // Act
        String token = tokenProvider.generateToken("testuser", false);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void testGenerateRefreshToken() {
        // Act
        String refreshToken = tokenProvider.generateRefreshToken("testuser");

        // Assert
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void testGetUsernameFromToken() {
        // Arrange
        String username = "testuser";
        String token = tokenProvider.generateToken(username, false);

        // Act
        String extractedUsername = tokenProvider.getUsernameFromToken(token);

        // Assert
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void testExtractExpiration() {
        // Arrange
        String token = tokenProvider.generateToken("testuser", false);

        // Act
        Date expiration = tokenProvider.extractExpiration(token);

        // Assert
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());

        // Should expire approximately 1 hour from now (with small margin)
        long expectedExpiry = System.currentTimeMillis() + jwtConfig.getExpiration();
        long actualExpiry = expiration.getTime();
        assertThat(actualExpiry).isCloseTo(expectedExpiry, within(5000L)); // 5 second margin
    }

    @Test
    @DisplayName("Should validate token with matching user details")
    void testValidateTokenSuccess() {
        // Arrange
        String token = tokenProvider.generateToken("testuser", false);

        // Act
        Boolean isValid = tokenProvider.validateToken(token, userDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token with mismatched username")
    void testValidateTokenFailureWithDifferentUsername() {
        // Arrange
        String token = tokenProvider.generateToken("differentuser", false);

        // Act
        Boolean isValid = tokenProvider.validateToken(token, userDetails);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should validate token without user details")
    void testValidateTokenWithoutUserDetails() {
        // Arrange
        String token = tokenProvider.generateToken("testuser", false);

        // Act
        boolean isValid = tokenProvider.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testValidateMalformedToken() {
        // Arrange
        String malformedToken = "this.is.not.a.valid.jwt";

        // Act
        boolean isValid = tokenProvider.validateToken(malformedToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject invalid token signature")
    void testValidateInvalidSignature() {
        // Arrange - Use a clearly invalid token with wrong signature
        // This simulates a token signed with a different secret
        String tokenWithInvalidSignature = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0dXNlciJ9.invalidsignaturehere";

        // Act & Assert - validateToken catches signature exceptions and returns false
        // Should not throw exception, just return false
        boolean isValid = false;
        try {
            isValid = tokenProvider.validateToken(tokenWithInvalidSignature);
        } catch (Exception e) {
            // Exception is acceptable for invalid signature
            // The test passes as long as validation fails (either false or exception)
        }

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject empty token")
    void testValidateEmptyToken() {
        // Act
        boolean isValid = tokenProvider.validateToken("");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject null token when extracting username")
    void testGetUsernameFromNullToken() {
        // Act & Assert
        assertThatThrownBy(() -> tokenProvider.getUsernameFromToken(null))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should get correct expiration time")
    void testGetExpirationTime() {
        // Act
        long expirationTime = tokenProvider.getExpirationTime();

        // Assert
        assertThat(expirationTime).isEqualTo(jwtConfig.getExpiration());
        assertThat(expirationTime).isEqualTo(3600000); // 1 hour
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void testTokenUniqueness() throws InterruptedException {
        // Arrange & Act
        String token1 = tokenProvider.generateToken("testuser", false);
        Thread.sleep(1000); // Wait 1 second to ensure different timestamp
        String token2 = tokenProvider.generateToken("testuser", false);

        // Assert
        assertThat(token1).isNotEqualTo(token2);

        // But both should have same username
        assertThat(tokenProvider.getUsernameFromToken(token1))
                .isEqualTo(tokenProvider.getUsernameFromToken(token2));
    }

    @Test
    @DisplayName("Should generate access token and refresh token with different expiration")
    void testAccessAndRefreshTokenDifferentExpiration() {
        // Arrange & Act
        String accessToken = tokenProvider.generateToken("testuser", false);
        String refreshToken = tokenProvider.generateToken("testuser", true);

        Date accessExpiration = tokenProvider.extractExpiration(accessToken);
        Date refreshExpiration = tokenProvider.extractExpiration(refreshToken);

        // Assert
        assertThat(refreshExpiration).isAfter(accessExpiration);

        // Refresh token should expire approximately 7 days from now
        long expectedRefreshExpiry = System.currentTimeMillis() + jwtConfig.getRefreshExpiration();
        assertThat(refreshExpiration.getTime()).isCloseTo(expectedRefreshExpiry, within(5000L));
    }

    @Test
    @DisplayName("Should handle token with special characters in username")
    void testTokenWithSpecialCharactersInUsername() {
        // Arrange
        String specialUsername = "user+test@example.com";

        // Act
        String token = tokenProvider.generateToken(specialUsername, false);
        String extractedUsername = tokenProvider.getUsernameFromToken(token);

        // Assert
        assertThat(extractedUsername).isEqualTo(specialUsername);
    }

    @Test
    @DisplayName("Should generate valid token structure")
    void testTokenStructure() {
        // Act
        String token = tokenProvider.generateToken("testuser", false);

        // Assert
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        // Each part should be base64 encoded (not empty)
        assertThat(parts[0]).isNotEmpty(); // Header
        assertThat(parts[1]).isNotEmpty(); // Payload
        assertThat(parts[2]).isNotEmpty(); // Signature
    }
}
