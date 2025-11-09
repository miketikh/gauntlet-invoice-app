package com.invoiceme.builders;

import com.invoiceme.payment.commands.RecordPaymentDTO;
import com.invoiceme.payment.domain.Payment;
import com.invoiceme.payment.domain.PaymentMethod;
import com.invoiceme.payment.domain.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder for Payment test data
 */
public class PaymentTestBuilder {

    private UUID invoiceId = UUID.randomUUID();
    private LocalDate paymentDate = LocalDate.now();
    private BigDecimal amount = new BigDecimal("100.00");
    private PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
    private String reference = "TEST-REF-001";
    private String notes = null;
    private String createdBy = "testuser";
    private String idempotencyKey = null;

    private PaymentTestBuilder() {
    }

    public static PaymentTestBuilder create() {
        return new PaymentTestBuilder();
    }

    public PaymentTestBuilder withInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
        return this;
    }

    public PaymentTestBuilder withPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
        return this;
    }

    public PaymentTestBuilder withAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public PaymentTestBuilder withAmount(String amount) {
        this.amount = new BigDecimal(amount);
        return this;
    }

    public PaymentTestBuilder withPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    public PaymentTestBuilder withReference(String reference) {
        this.reference = reference;
        return this;
    }

    public PaymentTestBuilder withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public PaymentTestBuilder withCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public PaymentTestBuilder withIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public PaymentTestBuilder withDefaults() {
        this.invoiceId = UUID.randomUUID();
        this.paymentDate = LocalDate.now();
        this.amount = new BigDecimal("100.00");
        this.paymentMethod = PaymentMethod.CREDIT_CARD;
        this.reference = "TEST-REF-001";
        this.notes = null;
        this.createdBy = "testuser";
        this.idempotencyKey = null;
        return this;
    }

    /**
     * Build Payment entity
     */
    public Payment build() {
        return Payment.createPayment(
            invoiceId,
            paymentDate,
            amount,
            paymentMethod,
            reference,
            notes,
            createdBy
        );
    }

    /**
     * Build RecordPaymentDTO for API requests
     */
    public RecordPaymentDTO buildDTO() {
        return new RecordPaymentDTO(
            paymentDate,
            amount,
            paymentMethod,
            reference,
            notes,
            idempotencyKey
        );
    }

    /**
     * Build and save Payment to repository
     */
    public Payment buildAndSave(PaymentRepository repository) {
        Payment payment = build();
        return repository.save(payment);
    }

    /**
     * Build multiple Payment instances for the same invoice
     */
    public List<Payment> buildList(int count, UUID invoiceId) {
        List<Payment> payments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            payments.add(
                PaymentTestBuilder.create()
                    .withInvoiceId(invoiceId)
                    .withAmount(new BigDecimal("50.00"))
                    .withReference("TEST-REF-" + String.format("%03d", i + 1))
                    .withPaymentDate(LocalDate.now().minusDays(count - i - 1))
                    .build()
            );
        }
        return payments;
    }

    /**
     * Build and save multiple Payment instances
     */
    public List<Payment> buildAndSaveList(PaymentRepository repository, int count, UUID invoiceId) {
        List<Payment> payments = buildList(count, invoiceId);
        return repository.saveAll(payments);
    }
}
