package com.invoiceme.common.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * IdempotencyService
 * Service for managing idempotency of requests
 * Prevents duplicate processing by caching results with idempotency keys
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final int DEFAULT_TTL_HOURS = 24;

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Checks if an idempotency key exists and returns cached result if available
     *
     * @param idempotencyKey The idempotency key to check
     * @param resultType The class type of the expected result
     * @return Optional containing cached result if key exists and not expired
     */
    @Transactional(readOnly = true)
    public <T> Optional<T> checkIdempotency(String idempotencyKey, Class<T> resultType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        return repository.findByIdempotencyKey(idempotencyKey)
            .filter(record -> !record.isExpired())
            .map(record -> {
                try {
                    log.info("Idempotency key found: {} - Returning cached result", idempotencyKey);
                    return objectMapper.readValue(record.getResult(), resultType);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize cached result for key: {}", idempotencyKey, e);
                    return null;
                }
            });
    }

    /**
     * Stores a result with an idempotency key
     * Uses a separate transaction to ensure idempotency record is persisted
     * even if outer transaction fails
     *
     * @param idempotencyKey The idempotency key
     * @param result The result to cache
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void storeIdempotency(String idempotencyKey, Object result) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        try {
            String jsonResult = objectMapper.writeValueAsString(result);
            IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, jsonResult, DEFAULT_TTL_HOURS);
            repository.save(record);
            log.info("Stored idempotency record with key: {}", idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize result for idempotency key: {}", idempotencyKey, e);
            // Don't throw - idempotency is a nice-to-have, not critical
        }
    }

    /**
     * Cleans up expired idempotency records
     * Should be called periodically (e.g., via scheduled task)
     *
     * @return Number of deleted records
     */
    @Transactional
    public int cleanupExpiredRecords() {
        int deleted = repository.deleteExpiredRecords(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency records", deleted);
        }
        return deleted;
    }
}
