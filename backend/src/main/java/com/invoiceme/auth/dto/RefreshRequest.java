package com.invoiceme.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token refresh request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}