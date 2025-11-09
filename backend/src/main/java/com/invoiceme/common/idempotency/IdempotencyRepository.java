package com.invoiceme.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * IdempotencyRepository
 * Repository for managing idempotency records
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    /**
     * Finds an idempotency record by its key
     *
     * @param idempotencyKey The idempotency key
     * @return Optional containing the record if found
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Deletes expired idempotency records
     * Should be called periodically to clean up old records
     *
     * @param now The current timestamp
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM IdempotencyRecord i WHERE i.expiresAt < :now")
    int deleteExpiredRecords(LocalDateTime now);
}
