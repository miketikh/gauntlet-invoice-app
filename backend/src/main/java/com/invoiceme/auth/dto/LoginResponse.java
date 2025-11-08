package com.invoiceme.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response DTO containing JWT tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType = "Bearer";
}