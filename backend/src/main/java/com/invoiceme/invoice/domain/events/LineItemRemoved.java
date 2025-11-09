package com.invoiceme.invoice.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * LineItemRemoved Event
 * Published when a line item is removed from an invoice
 */
public record LineItemRemoved(
    UUID eventId,
    Instant occurredAt,
    UUID invoiceId,
    String lineItemId
) implements DomainEvent {

    public LineItemRemoved(UUID invoiceId, String lineItemId) {
        this(UUID.randomUUID(), Instant.now(), invoiceId, lineItemId);
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
        return "LineItemRemoved";
    }
}
