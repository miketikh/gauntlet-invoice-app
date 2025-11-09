package com.invoiceme.customer.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Event: Published when a new customer is created
 */
public record CustomerCreated(
    UUID customerId,
    String name,
    String email,
    LocalDateTime occurredAt
) {
    public CustomerCreated(UUID customerId, String name, String email) {
        this(customerId, name, email, LocalDateTime.now());
    }
}
