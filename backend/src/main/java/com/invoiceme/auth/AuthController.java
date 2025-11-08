package com.invoiceme.auth;

import com.invoiceme.auth.dto.LoginRequest;
import com.invoiceme.auth.dto.LoginResponse;
import com.invoiceme.auth.dto.RefreshRequest;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication REST controller
 * Handles login and token refresh endpoints
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    /**
     * Login endpoint
     * Authenticates user and returns JWT tokens
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(loginRequest.getUsername());

            return ResponseEntity.ok(LoginResponse.builder()
                    .token(jwt)
                    .refreshToken(refreshToken)
                    .expiresIn(tokenProvider.getExpirationTime())
                    .tokenType("Bearer")
                    .build());

        } catch (BadCredentialsException ex) {
            log.error("Invalid credentials for user: {}", loginRequest.getUsername());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        } catch (Exception ex) {
            log.error("Authentication failed: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Authentication failed");
        }
    }

    /**
     * Refresh token endpoint
     * Validates refresh token and returns new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshRequest refreshRequest) {
        try {
            String refreshToken = refreshRequest.getRefreshToken();

            if (tokenProvider.validateToken(refreshToken)) {
                String username = tokenProvider.getUsernameFromToken(refreshToken);
                String newAccessToken = tokenProvider.generateToken(username, false);
                String newRefreshToken = tokenProvider.generateRefreshToken(username);

                return ResponseEntity.ok(LoginResponse.builder()
                        .token(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .expiresIn(tokenProvider.getExpirationTime())
                        .tokenType("Bearer")
                        .build());
            } else {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid refresh token");
            }
        } catch (ExpiredJwtException ex) {
            log.error("Refresh token expired");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token expired");
        } catch (Exception ex) {
            log.error("Token refresh failed: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Token refresh failed");
        }
    }

    /**
     * Health check endpoint to verify auth service is running
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}