package com.invoiceme.auth;

import com.invoiceme.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JWT token provider for generating and validating tokens
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    /**
     * Generate JWT token from Authentication
     */
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateToken(userPrincipal.getUsername(), false);
    }

    /**
     * Generate JWT token from username
     */
    public String generateToken(String username, boolean isRefreshToken) {
        Map<String, Object> claims = new HashMap<>();
        long expiration = isRefreshToken ?
            jwtConfig.getRefreshExpiration() :
            jwtConfig.getExpiration();

        return createToken(claims, username, expiration);
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String username) {
        return generateToken(username, true);
    }

    /**
     * Create token with claims
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Get username from token
     */
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token against user details
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Validate token (simple validation without user details)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("JWT token is expired");
        } catch (UnsupportedJwtException ex) {
            log.error("JWT token is unsupported");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Get token expiration in milliseconds
     */
    public long getExpirationTime() {
        return jwtConfig.getExpiration();
    }
}