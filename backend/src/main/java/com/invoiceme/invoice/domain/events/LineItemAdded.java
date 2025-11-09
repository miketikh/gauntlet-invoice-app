package com.invoiceme.invoice.domain.events;

import com.invoiceme.invoice.domain.LineItem;

import java.time.Instant;
import java.util.UUID;

/**
 * LineItemAdded Event
 * Published when a line item is added to an invoice
 */
public record LineItemAdded(
    UUID eventId,
    Instant occurredAt,
    UUID invoiceId,
    LineItem lineItem
) implements DomainEvent {

    public LineItemAdded(UUID invoiceId, LineItem lineItem) {
        this(UUID.randomUUID(), Instant.now(), invoiceId, lineItem);
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
        return "LineItemAdded";
    }
}
