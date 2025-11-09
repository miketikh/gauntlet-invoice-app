package com.invoiceme.customer.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Event: Published when a customer is updated
 */
public record CustomerUpdated(
    UUID customerId,
    String name,
    String email,
    LocalDateTime occurredAt
) {
    public CustomerUpdated(UUID customerId, String name, String email) {
        this(customerId, name, email, LocalDateTime.now());
    }
}
