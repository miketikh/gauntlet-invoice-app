package com.invoiceme.payment.exceptions;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * PaymentExceedsBalanceException
 * Thrown when a payment amount exceeds the invoice balance
 * Business rule: Payment amount cannot be greater than remaining balance
 */
public class PaymentExceedsBalanceException extends RuntimeException {

    private final UUID invoiceId;
    private final BigDecimal paymentAmount;
    private final BigDecimal balance;

    public PaymentExceedsBalanceException(UUID invoiceId, BigDecimal paymentAmount, BigDecimal balance) {
        super(String.format("Payment amount ($%s) exceeds invoice balance ($%s). Invoice ID: %s",
            paymentAmount, balance, invoiceId));
        this.invoiceId = invoiceId;
        this.paymentAmount = paymentAmount;
        this.balance = balance;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
