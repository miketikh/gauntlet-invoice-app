package com.invoiceme.payment;

import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.payment.domain.Payment;
import com.invoiceme.payment.queries.PaymentResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PaymentMapper
 * Maps Payment domain entities to DTOs
 */
@Component
public class PaymentMapper {

    /**
     * Maps Payment entity to PaymentResponseDTO with invoice and customer data
     *
     * @param payment The payment entity
     * @param invoice The related invoice (for enriched data)
     * @param customerName The customer name from invoice's customer
     * @return PaymentResponseDTO with all fields populated
     */
    public PaymentResponseDTO toResponseDTO(Payment payment, Invoice invoice, String customerName) {
        if (payment == null) {
            return null;
        }

        String invoiceNumber = invoice != null ? invoice.getInvoiceNumber() : null;
        BigDecimal remainingBalance = invoice != null ? invoice.getBalance() : null;

        return new PaymentResponseDTO(
            payment.getId(),
            payment.getInvoiceId(),
            payment.getPaymentDate(),
            payment.getAmount(),
            payment.getPaymentMethod(),
            payment.getReference(),
            payment.getNotes(),
            payment.getCreatedAt(),
            payment.getCreatedBy(),
            invoiceNumber,
            customerName,
            remainingBalance
        );
    }

    /**
     * Maps Payment entity to PaymentResponseDTO without enriched data
     * Used when invoice/customer data is not available or not needed
     *
     * @param payment The payment entity
     * @return PaymentResponseDTO with basic fields populated
     */
    public PaymentResponseDTO toResponseDTO(Payment payment) {
        return toResponseDTO(payment, null, null);
    }
}
