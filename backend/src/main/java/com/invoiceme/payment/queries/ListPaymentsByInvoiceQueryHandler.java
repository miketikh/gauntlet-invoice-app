package com.invoiceme.payment.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.payment.domain.Payment;
import com.invoiceme.payment.domain.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ListPaymentsByInvoiceQueryHandler
 * Handler for retrieving all payments for a specific invoice with running balance calculation
 */
@Service
public class ListPaymentsByInvoiceQueryHandler {

    private static final Logger logger = LoggerFactory.getLogger(ListPaymentsByInvoiceQueryHandler.class);

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public ListPaymentsByInvoiceQueryHandler(
        PaymentRepository paymentRepository,
        InvoiceRepository invoiceRepository,
        CustomerRepository customerRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Handles the ListPaymentsByInvoiceQuery
     * Returns payments sorted chronologically with running balance calculation
     *
     * @param query The query containing invoice ID
     * @return List of PaymentResponseDTO with running balances
     * @throws EntityNotFoundException if invoice or customer not found
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> handle(ListPaymentsByInvoiceQuery query) {
        logger.debug("Retrieving payments for invoice: {}", query.invoiceId());

        // Fetch invoice to validate existence and get customer info
        Invoice invoice = invoiceRepository.findById(query.invoiceId())
            .orElseThrow(() -> {
                logger.warn("Invoice not found: {}", query.invoiceId());
                return new EntityNotFoundException(
                    "Invoice not found with id: " + query.invoiceId()
                );
            });

        // Fetch customer
        Customer customer = customerRepository.findById(invoice.getCustomerId())
            .orElseThrow(() -> {
                logger.error("Customer not found for invoice: {}, customerId: {}",
                    query.invoiceId(), invoice.getCustomerId());
                return new EntityNotFoundException(
                    "Customer not found with id: " + invoice.getCustomerId()
                );
            });

        // Fetch payments sorted chronologically (oldest first)
        List<Payment> payments = paymentRepository.findByInvoiceIdOrderByPaymentDateAsc(query.invoiceId());

        // Handle empty list gracefully
        if (payments.isEmpty()) {
            logger.info("No payments found for invoice {}", query.invoiceId());
            return Collections.emptyList();
        }

        // Calculate running balance for each payment
        List<PaymentResponseDTO> result = calculateRunningBalances(payments, invoice, customer);

        logger.info("Retrieved {} payments for invoice {}", payments.size(), query.invoiceId());

        return result;
    }

    /**
     * Calculates running balance for each payment
     * Running balance = invoice total - cumulative payments
     *
     * @param payments List of payments in chronological order
     * @param invoice The invoice
     * @param customer The customer
     * @return List of PaymentResponseDTOs with running balances
     */
    private List<PaymentResponseDTO> calculateRunningBalances(
        List<Payment> payments,
        Invoice invoice,
        Customer customer
    ) {
        List<PaymentResponseDTO> result = new ArrayList<>();
        BigDecimal runningBalance = invoice.getTotalAmount();

        for (Payment payment : payments) {
            // Subtract payment amount from running balance
            runningBalance = runningBalance.subtract(payment.getAmount());

            // Create DTO with running balance
            PaymentResponseDTO dto = PaymentMapper.toResponseDTO(
                payment,
                invoice,
                customer,
                runningBalance
            );

            result.add(dto);
        }

        return result;
    }
}
