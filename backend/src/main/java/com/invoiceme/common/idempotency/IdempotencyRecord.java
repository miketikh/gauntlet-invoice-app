package com.invoiceme.common.idempotency;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * IdempotencyRecord
 * Entity for tracking idempotent requests to prevent duplicate processing
 * Stores the result of a request by its idempotency key with expiration
 */
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_idempotency_expires_at", columnList = "expiresAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String result;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Creates a new IdempotencyRecord
     *
     * @param idempotencyKey The unique idempotency key
     * @param result The JSON result to cache
     * @param ttlHours Time to live in hours
     */
    public IdempotencyRecord(String idempotencyKey, String result, int ttlHours) {
        this.idempotencyKey = idempotencyKey;
        this.result = result;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(ttlHours);
    }

    /**
     * Checks if this record has expired
     *
     * @return true if the record is past its expiration time
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
