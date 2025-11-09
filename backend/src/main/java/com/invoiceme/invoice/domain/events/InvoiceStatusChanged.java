package com.invoiceme.invoice.domain.events;

import com.invoiceme.invoice.domain.InvoiceStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * InvoiceStatusChanged Event
 * Published when an invoice changes status (Draft -> Sent -> Paid)
 */
public record InvoiceStatusChanged(
    UUID eventId,
    Instant occurredAt,
    UUID invoiceId,
    InvoiceStatus oldStatus,
    InvoiceStatus newStatus
) implements DomainEvent {

    public InvoiceStatusChanged(UUID invoiceId, InvoiceStatus oldStatus, InvoiceStatus newStatus) {
        this(UUID.randomUUID(), Instant.now(), invoiceId, oldStatus, newStatus);
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
        return "InvoiceStatusChanged";
    }
}
