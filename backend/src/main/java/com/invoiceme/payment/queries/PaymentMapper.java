package com.invoiceme.payment.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.payment.domain.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PaymentMapper
 * Utility class for mapping Payment entities to PaymentResponseDTO
 */
public class PaymentMapper {

    private PaymentMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Maps a Payment entity to PaymentResponseDTO with enriched invoice and customer data
     *
     * @param payment Payment entity
     * @param invoice Related invoice
     * @param customer Related customer
     * @return PaymentResponseDTO with all fields populated
     */
    public static PaymentResponseDTO toResponseDTO(Payment payment, Invoice invoice, Customer customer) {
        return toResponseDTO(payment, invoice, customer, null);
    }

    /**
     * Maps a Payment entity to PaymentResponseDTO with enriched invoice and customer data
     * including optional running balance for payment history queries
     *
     * @param payment Payment entity
     * @param invoice Related invoice
     * @param customer Related customer
     * @param runningBalance Optional running balance (for payment history)
     * @return PaymentResponseDTO with all fields populated
     */
    public static PaymentResponseDTO toResponseDTO(
        Payment payment,
        Invoice invoice,
        Customer customer,
        BigDecimal runningBalance
    ) {
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

            // Invoice fields
            invoice.getInvoiceNumber(),
            invoice.getTotalAmount(),
            invoice.getBalance(),
            invoice.getStatus(),

            // Customer fields
            customer.getName(),
            customer.getEmail(),

            // Optional running balance
            runningBalance
        );
    }

    /**
     * Maps a list of Payment entities to PaymentResponseDTOs
     *
     * @param payments List of payment entities
     * @param invoice Related invoice
     * @param customer Related customer
     * @return List of PaymentResponseDTOs
     */
    public static List<PaymentResponseDTO> toResponseDTOList(
        List<Payment> payments,
        Invoice invoice,
        Customer customer
    ) {
        return payments.stream()
            .map(payment -> toResponseDTO(payment, invoice, customer))
            .collect(Collectors.toList());
    }
}
