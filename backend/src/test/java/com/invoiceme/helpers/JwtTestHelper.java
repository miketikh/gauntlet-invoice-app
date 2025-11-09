package com.invoiceme.helpers;

import com.invoiceme.auth.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Helper class for JWT token operations in tests
 */
@Component
public class JwtTestHelper {

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public JwtTestHelper(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Generate a valid JWT token for testing
     * @param username Username to include in token
     * @return JWT token string
     */
    public String generateToken(String username) {
        return jwtTokenProvider.generateToken(username, false);
    }

    /**
     * Get Authorization header with Bearer token
     * @param username Username to generate token for
     * @return Authorization header value
     */
    public String getAuthHeader(String username) {
        return "Bearer " + generateToken(username);
    }

    /**
     * Extract username from JWT token
     * @param token JWT token
     * @return Username from token
     */
    public String extractUsername(String token) {
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    /**
     * Validate JWT token
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            jwtTokenProvider.validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
