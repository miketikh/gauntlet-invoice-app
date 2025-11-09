package com.invoiceme.invoice.domain;

import com.invoiceme.invoice.domain.events.*;
import com.invoiceme.invoice.domain.exceptions.InvalidInvoiceStateException;
import com.invoiceme.invoice.domain.exceptions.InvoiceImmutableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Invoice aggregate root
 */
@DisplayName("Invoice Aggregate Tests")
class InvoiceTest {

    private UUID customerId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String invoiceNumber;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        issueDate = LocalDate.now();
        dueDate = LocalDate.now().plusDays(30);
        invoiceNumber = "INV-2024-0001";
    }

    @Test
    @DisplayName("Should create invoice with valid data")
    void shouldCreateInvoiceWithValidData() {
        // Act
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);

        // Assert
        assertThat(invoice).isNotNull();
        assertThat(invoice.getCustomerId()).isEqualTo(customerId);
        assertThat(invoice.getIssueDate()).isEqualTo(issueDate);
        assertThat(invoice.getDueDate()).isEqualTo(dueDate);
        assertThat(invoice.getPaymentTerms()).isEqualTo("Net 30");
        assertThat(invoice.getInvoiceNumber()).isEqualTo(invoiceNumber);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.Draft);
        assertThat(invoice.getLineItems()).isEmpty();
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should publish InvoiceCreated event on creation")
    void shouldPublishInvoiceCreatedEvent() {
        // Act
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        List<DomainEvent> events = invoice.getDomainEvents();

        // Assert
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(InvoiceCreated.class);
        InvoiceCreated event = (InvoiceCreated) events.get(0);
        assertThat(event.customerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Should throw exception when due date is before issue date")
    void shouldThrowExceptionForInvalidDueDate() {
        // Arrange
        LocalDate invalidDueDate = issueDate.minusDays(1);

        // Act & Assert
        assertThatThrownBy(() -> Invoice.create(customerId, issueDate, invalidDueDate, "Net 30", invoiceNumber))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Due date must be on or after issue date");
    }

    @Test
    @DisplayName("Should add line item and recalculate totals")
    void shouldAddLineItemAndRecalculateTotals() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(
            null,
            "Software License",
            5,
            new BigDecimal("100.00"),
            new BigDecimal("0.10"),
            new BigDecimal("0.08")
        );

        // Act
        invoice.addLineItem(lineItem);

        // Assert
        assertThat(invoice.getLineItems()).hasSize(1);
        assertThat(invoice.getSubtotal()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(invoice.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(invoice.getTotalTax()).isEqualByComparingTo(new BigDecimal("36.00"));
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("486.00"));
        assertThat(invoice.getBalance()).isEqualByComparingTo(new BigDecimal("486.00"));
    }

    @Test
    @DisplayName("Should publish LineItemAdded event when adding line item")
    void shouldPublishLineItemAddedEvent() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        invoice.getDomainEvents(); // Clear creation event
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // Act
        invoice.addLineItem(lineItem);
        List<DomainEvent> events = invoice.getDomainEvents();

        // Assert
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(LineItemAdded.class);
    }

    @Test
    @DisplayName("Should throw exception when adding line item to sent invoice")
    void shouldThrowExceptionWhenAddingLineItemToSentInvoice() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem1 = new LineItem(null, "Item 1", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem1);
        invoice.markAsSent();

        LineItem lineItem2 = new LineItem(null, "Item 2", 1, new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // Act & Assert
        assertThatThrownBy(() -> invoice.addLineItem(lineItem2))
            .isInstanceOf(InvoiceImmutableException.class)
            .hasMessageContaining("Cannot modify line items after invoice is sent");
    }

    @Test
    @DisplayName("Should remove line item and recalculate totals")
    void shouldRemoveLineItemAndRecalculateTotals() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem1 = new LineItem("item-1", "Item 1", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        LineItem lineItem2 = new LineItem("item-2", "Item 2", 1, new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem1);
        invoice.addLineItem(lineItem2);

        // Act
        invoice.removeLineItem("item-1");

        // Assert
        assertThat(invoice.getLineItems()).hasSize(1);
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Should publish LineItemRemoved event when removing line item")
    void shouldPublishLineItemRemovedEvent() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem("item-1", "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.getDomainEvents(); // Clear events

        // Act
        invoice.removeLineItem("item-1");
        List<DomainEvent> events = invoice.getDomainEvents();

        // Assert
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(LineItemRemoved.class);
    }

    @Test
    @DisplayName("Should throw exception when removing line item from sent invoice")
    void shouldThrowExceptionWhenRemovingLineItemFromSentInvoice() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem("item-1", "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.markAsSent();

        // Act & Assert
        assertThatThrownBy(() -> invoice.removeLineItem("item-1"))
            .isInstanceOf(InvoiceImmutableException.class)
            .hasMessageContaining("Cannot modify line items after invoice is sent");
    }

    @Test
    @DisplayName("Should update line item and recalculate totals")
    void shouldUpdateLineItemAndRecalculateTotals() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem("item-1", "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);

        LineItem updatedLineItem = new LineItem("item-1", "Updated Item", 2, new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // Act
        invoice.updateLineItem("item-1", updatedLineItem);

        // Assert
        assertThat(invoice.getLineItems()).hasSize(1);
        assertThat(invoice.getLineItems().get(0).description()).isEqualTo("Updated Item");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Should throw exception when updating line item in sent invoice")
    void shouldThrowExceptionWhenUpdatingLineItemInSentInvoice() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem("item-1", "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.markAsSent();

        LineItem updatedLineItem = new LineItem("item-1", "Updated", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        // Act & Assert
        assertThatThrownBy(() -> invoice.updateLineItem("item-1", updatedLineItem))
            .isInstanceOf(InvoiceImmutableException.class)
            .hasMessageContaining("Cannot modify line items after invoice is sent");
    }

    @Test
    @DisplayName("Should mark invoice as sent")
    void shouldMarkInvoiceAsSent() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);

        // Act
        invoice.markAsSent();

        // Assert
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.Sent);
    }

    @Test
    @DisplayName("Should publish InvoiceStatusChanged event when marking as sent")
    void shouldPublishStatusChangedEventWhenMarkingAsSent() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.getDomainEvents(); // Clear events

        // Act
        invoice.markAsSent();
        List<DomainEvent> events = invoice.getDomainEvents();

        // Assert
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(InvoiceStatusChanged.class);
        InvoiceStatusChanged event = (InvoiceStatusChanged) events.get(0);
        assertThat(event.oldStatus()).isEqualTo(InvoiceStatus.Draft);
        assertThat(event.newStatus()).isEqualTo(InvoiceStatus.Sent);
    }

    @Test
    @DisplayName("Should throw exception when marking invoice as sent without line items")
    void shouldThrowExceptionWhenMarkingAsSentWithoutLineItems() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);

        // Act & Assert
        assertThatThrownBy(() -> invoice.markAsSent())
            .isInstanceOf(InvalidInvoiceStateException.class)
            .hasMessageContaining("Cannot send invoice without line items");
    }

    @Test
    @DisplayName("Should mark invoice as paid when balance is zero")
    void shouldMarkInvoiceAsPaid() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.markAsSent();
        invoice.applyPayment(new BigDecimal("100.00"));

        // Act
        // Invoice should be automatically marked as paid after payment

        // Assert
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.Paid);
        assertThat(invoice.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should throw exception when marking as paid with outstanding balance")
    void shouldThrowExceptionWhenMarkingAsPaidWithBalance() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.markAsSent();

        // Act & Assert
        assertThatThrownBy(() -> invoice.markAsPaid())
            .isInstanceOf(InvalidInvoiceStateException.class)
            .hasMessageContaining("Cannot mark as Paid with outstanding balance");
    }

    @Test
    @DisplayName("Should throw exception when marking draft invoice as paid")
    void shouldThrowExceptionWhenMarkingDraftAsPaid() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);

        // Act & Assert
        assertThatThrownBy(() -> invoice.markAsPaid())
            .isInstanceOf(InvalidInvoiceStateException.class)
            .hasMessageContaining("Can only mark Sent invoices as Paid");
    }

    @Test
    @DisplayName("Should apply payment and reduce balance")
    void shouldApplyPaymentAndReduceBalance() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.markAsSent();

        // Act
        invoice.applyPayment(new BigDecimal("30.00"));

        // Assert
        assertThat(invoice.getBalance()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.Sent);
    }

    @Test
    @DisplayName("Should auto-transition to Paid when balance reaches zero")
    void shouldAutoTransitionToPaidWhenBalanceZero() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.markAsSent();

        // Act
        invoice.applyPayment(new BigDecimal("100.00"));

        // Assert
        assertThat(invoice.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.Paid);
    }

    @Test
    @DisplayName("Should throw exception when payment exceeds balance")
    void shouldThrowExceptionWhenPaymentExceedsBalance() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.markAsSent();

        // Act & Assert
        assertThatThrownBy(() -> invoice.applyPayment(new BigDecimal("150.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payment amount cannot exceed invoice balance");
    }

    @Test
    @DisplayName("Should throw exception for negative payment")
    void shouldThrowExceptionForNegativePayment() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.markAsSent();

        // Act & Assert
        assertThatThrownBy(() -> invoice.applyPayment(new BigDecimal("-10.00")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Payment amount must be positive");
    }

    @Test
    @DisplayName("Should calculate totals with multiple line items")
    void shouldCalculateTotalsWithMultipleLineItems() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);

        LineItem item1 = new LineItem(null, "Item 1", 5, new BigDecimal("100.00"), new BigDecimal("0.10"), new BigDecimal("0.08"));
        LineItem item2 = new LineItem(null, "Item 2", 2, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("0.08"));

        // Act
        invoice.addLineItem(item1);
        invoice.addLineItem(item2);

        // Assert
        // Item 1: subtotal=500, discount=50, taxable=450, tax=36, total=486
        // Item 2: subtotal=100, discount=0, taxable=100, tax=8, total=108
        // Combined: subtotal=600, discount=50, tax=44, total=594
        assertThat(invoice.getSubtotal()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(invoice.getTotalDiscount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(invoice.getTotalTax()).isEqualByComparingTo(new BigDecimal("44.00"));
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("594.00"));
    }

    @Test
    @DisplayName("Should return true for canBeSent when draft with line items")
    void shouldReturnTrueForCanBeSent() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);

        // Act & Assert
        assertThat(invoice.canBeSent()).isTrue();
    }

    @Test
    @DisplayName("Should return false for canBeSent when no line items")
    void shouldReturnFalseForCanBeSentWithoutLineItems() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);

        // Act & Assert
        assertThat(invoice.canBeSent()).isFalse();
    }

    @Test
    @DisplayName("Should return true for canBePaid when sent with balance")
    void shouldReturnTrueForCanBePaid() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);
        LineItem lineItem = new LineItem(null, "Item", 1, new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO);
        invoice.addLineItem(lineItem);
        invoice.markAsSent();

        // Act & Assert
        assertThat(invoice.canBePaid()).isTrue();
    }

    @Test
    @DisplayName("Should return false for canBePaid when draft")
    void shouldReturnFalseForCanBePaidWhenDraft() {
        // Arrange
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, "Net 30", invoiceNumber);

        // Act & Assert
        assertThat(invoice.canBePaid()).isFalse();
    }
}
