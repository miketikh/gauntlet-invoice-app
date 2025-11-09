package com.invoiceme.invoice.domain;

import com.invoiceme.invoice.domain.events.*;
import com.invoiceme.invoice.domain.exceptions.InvalidInvoiceStateException;
import com.invoiceme.invoice.domain.exceptions.InvoiceImmutableException;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Invoice Aggregate Root
 * Represents an invoice with line items following DDD aggregate pattern
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoices_customer_id", columnList = "customerId"),
    @Index(name = "idx_invoices_status", columnList = "status"),
    @Index(name = "idx_invoices_due_date", columnList = "dueDate"),
    @Index(name = "idx_invoices_invoice_number", columnList = "invoiceNumber")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String invoiceNumber;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(length = 100)
    private String paymentTerms;

    // Line items stored as JSONB
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<LineItem> lineItems = new ArrayList<>();

    // Calculated totals (denormalized for query performance)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // Optimistic locking version
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // Domain events (transient - not persisted)
    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Factory method to create a new Invoice
     * @param customerId The customer ID
     * @param issueDate Invoice issue date
     * @param dueDate Payment due date
     * @param paymentTerms Payment terms description
     * @param invoiceNumber Generated invoice number
     * @return New Invoice instance in Draft status
     */
    public static Invoice create(UUID customerId, LocalDate issueDate, LocalDate dueDate,
                                  String paymentTerms, String invoiceNumber) {
        Invoice invoice = new Invoice();
        invoice.customerId = customerId;
        invoice.issueDate = issueDate;
        invoice.dueDate = dueDate;
        invoice.paymentTerms = paymentTerms;
        invoice.invoiceNumber = invoiceNumber;
        invoice.status = InvoiceStatus.Draft;
        invoice.lineItems = new ArrayList<>();
        invoice.validate();

        // Publish creation event
        invoice.domainEvents.add(new InvoiceCreated(invoice.id, customerId));

        return invoice;
    }

    /**
     * Adds a line item to the invoice
     * @param lineItem The line item to add
     * @throws InvoiceImmutableException if invoice is not in Draft status
     */
    public void addLineItem(LineItem lineItem) {
        if (status != InvoiceStatus.Draft) {
            throw new InvoiceImmutableException("Cannot modify line items after invoice is sent");
        }
        this.lineItems.add(lineItem);
        calculateTotals();
        domainEvents.add(new LineItemAdded(this.id, lineItem));
    }

    /**
     * Removes a line item from the invoice
     * @param lineItemId The ID of the line item to remove
     * @throws InvoiceImmutableException if invoice is not in Draft status
     */
    public void removeLineItem(String lineItemId) {
        if (status != InvoiceStatus.Draft) {
            throw new InvoiceImmutableException("Cannot modify line items after invoice is sent");
        }
        boolean removed = lineItems.removeIf(item -> item.id().equals(lineItemId));
        if (removed) {
            calculateTotals();
            domainEvents.add(new LineItemRemoved(this.id, lineItemId));
        }
    }

    /**
     * Updates a line item in the invoice
     * @param lineItemId The ID of the line item to update
     * @param updatedLineItem The updated line item
     * @throws InvoiceImmutableException if invoice is not in Draft status
     */
    public void updateLineItem(String lineItemId, LineItem updatedLineItem) {
        if (status != InvoiceStatus.Draft) {
            throw new InvoiceImmutableException("Cannot modify line items after invoice is sent");
        }
        for (int i = 0; i < lineItems.size(); i++) {
            if (lineItems.get(i).id().equals(lineItemId)) {
                lineItems.set(i, updatedLineItem);
                calculateTotals();
                return;
            }
        }
        throw new IllegalArgumentException("Line item not found: " + lineItemId);
    }

    /**
     * Marks invoice as sent - transitions from Draft to Sent
     * Initializes balance to equal totalAmount when first sent
     * @throws InvalidInvoiceStateException if invoice cannot be sent
     */
    public void markAsSent() {
        if (!canBeSent()) {
            throw new InvalidInvoiceStateException("Cannot send invoice without line items");
        }
        if (this.totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new InvalidInvoiceStateException("Cannot send invoice with zero total amount");
        }
        InvoiceStatus oldStatus = this.status;
        this.status = InvoiceStatus.Sent;
        // Initialize balance when invoice is first sent (balance was set by calculateTotals)
        // The balance field is already set to totalAmount by calculateTotals() method
        domainEvents.add(new InvoiceStatusChanged(this.id, oldStatus, InvoiceStatus.Sent));
    }

    /**
     * Marks invoice as paid - transitions from Sent to Paid
     * @throws InvalidInvoiceStateException if invoice cannot be marked as paid
     */
    public void markAsPaid() {
        if (status != InvoiceStatus.Sent) {
            throw new InvalidInvoiceStateException("Can only mark Sent invoices as Paid");
        }
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidInvoiceStateException("Cannot mark as Paid with outstanding balance");
        }
        InvoiceStatus oldStatus = this.status;
        this.status = InvoiceStatus.Paid;
        domainEvents.add(new InvoiceStatusChanged(this.id, oldStatus, InvoiceStatus.Paid));
    }

    /**
     * Checks if invoice can accept payment
     * @return true if invoice is in Sent status
     */
    public boolean canAcceptPayment() {
        return this.status == InvoiceStatus.Sent;
    }

    /**
     * Applies a payment to reduce the invoice balance
     * Auto-transitions to Paid when balance reaches zero
     * @param paymentAmount The payment amount
     * @throws IllegalArgumentException if payment amount is invalid
     */
    public void applyPayment(BigDecimal paymentAmount) {
        if (!canAcceptPayment()) {
            throw new IllegalArgumentException("Invoice must be in Sent status to accept payments");
        }
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (paymentAmount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed invoice balance");
        }
        this.balance = this.balance.subtract(paymentAmount);

        // Auto-transition to Paid when balance reaches zero
        if (this.balance.compareTo(BigDecimal.ZERO) == 0 && this.status == InvoiceStatus.Sent) {
            markAsPaid();
        }
    }

    /**
     * Checks if invoice can be sent
     * @return true if invoice has at least one line item and is in Draft status
     */
    public boolean canBeSent() {
        return status == InvoiceStatus.Draft && !lineItems.isEmpty();
    }

    /**
     * Checks if invoice can be paid
     * @return true if invoice is Sent and has a balance
     */
    public boolean canBePaid() {
        return status == InvoiceStatus.Sent && balance.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Sets the notes field
     * @param notes Invoice notes
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Updates invoice basic fields (only allowed in Draft status)
     * @param customerId Updated customer ID
     * @param issueDate Updated issue date
     * @param dueDate Updated due date
     * @param paymentTerms Updated payment terms
     * @param notes Updated notes
     * @throws InvoiceImmutableException if invoice is not in Draft status
     */
    public void updateFields(UUID customerId, LocalDate issueDate, LocalDate dueDate,
                            String paymentTerms, String notes) {
        if (status != InvoiceStatus.Draft) {
            throw new InvoiceImmutableException("Cannot modify invoice after it is sent");
        }

        this.customerId = customerId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.paymentTerms = paymentTerms;
        this.notes = notes;
        validate();
    }

    /**
     * Clears all line items (only allowed in Draft status)
     * @throws InvoiceImmutableException if invoice is not in Draft status
     */
    public void clearLineItems() {
        if (status != InvoiceStatus.Draft) {
            throw new InvoiceImmutableException("Cannot modify line items after invoice is sent");
        }
        this.lineItems.clear();
        calculateTotals();
    }

    /**
     * Returns a copy of domain events and clears the internal list
     * @return List of domain events
     */
    public List<DomainEvent> getDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    /**
     * Validates invoice data
     */
    private void validate() {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date is required");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date is required");
        }
        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Due date must be on or after issue date");
        }
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required");
        }
    }

    /**
     * Calculates all totals from line items
     */
    private void calculateTotals() {
        this.subtotal = lineItems.stream()
            .map(LineItem::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalDiscount = lineItems.stream()
            .map(LineItem::discountAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalTax = lineItems.stream()
            .map(LineItem::taxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalAmount = lineItems.stream()
            .map(LineItem::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Update balance to match totalAmount (payments handled separately)
        this.balance = this.totalAmount;
    }
}
