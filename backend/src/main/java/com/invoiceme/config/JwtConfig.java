package com.invoiceme.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties
 * Binds to application properties prefixed with "jwt"
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private long expiration = 3600000; // 1 hour in milliseconds
    private long refreshExpiration = 604800000; // 7 days in milliseconds
    private String tokenPrefix = "Bearer ";
    private String headerString = "Authorization";

    /**
     * Get expiration in seconds for JWT claims
     */
    public long getExpirationInSeconds() {
        return expiration / 1000;
    }

    /**
     * Get refresh expiration in seconds
     */
    public long getRefreshExpirationInSeconds() {
        return refreshExpiration / 1000;
    }
}