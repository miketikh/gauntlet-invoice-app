package com.invoiceme.invoice.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events
 */
public interface DomainEvent {
    UUID getEventId();
    Instant getOccurredAt();
    String getEventType();
}
