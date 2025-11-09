package com.invoiceme.customer.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Event: Published when a customer is soft-deleted
 */
public record CustomerDeleted(
    UUID customerId,
    LocalDateTime occurredAt
) {
    public CustomerDeleted(UUID customerId) {
        this(customerId, LocalDateTime.now());
    }
}
