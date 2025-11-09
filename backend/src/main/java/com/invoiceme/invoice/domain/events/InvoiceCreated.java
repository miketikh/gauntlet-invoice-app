package com.invoiceme.invoice.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * InvoiceCreated Event
 * Published when a new invoice is created
 */
public record InvoiceCreated(
    UUID eventId,
    Instant occurredAt,
    UUID invoiceId,
    UUID customerId
) implements DomainEvent {

    public InvoiceCreated(UUID invoiceId, UUID customerId) {
        this(UUID.randomUUID(), Instant.now(), invoiceId, customerId);
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getEventType() {
        return "InvoiceCreated";
    }
}
