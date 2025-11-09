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

/**
 * GetPaymentByIdQueryHandler
 * Handler for retrieving a single payment by ID with enriched invoice and customer data
 */
@Service
public class GetPaymentByIdQueryHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetPaymentByIdQueryHandler.class);

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public GetPaymentByIdQueryHandler(
        PaymentRepository paymentRepository,
        InvoiceRepository invoiceRepository,
        CustomerRepository customerRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Handles the GetPaymentByIdQuery
     *
     * @param query The query containing payment ID
     * @return PaymentResponseDTO with enriched data
     * @throws EntityNotFoundException if payment, invoice, or customer not found
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO handle(GetPaymentByIdQuery query) {
        logger.debug("Retrieving payment: {}", query.paymentId());

        // Fetch payment by ID
        Payment payment = paymentRepository.findById(query.paymentId())
            .orElseThrow(() -> {
                logger.warn("Payment not found: {}", query.paymentId());
                return new EntityNotFoundException(
                    "Payment not found with id: " + query.paymentId()
                );
            });

        // Fetch related invoice
        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
            .orElseThrow(() -> {
                logger.error("Invoice not found for payment: {}, invoiceId: {}",
                    query.paymentId(), payment.getInvoiceId());
                return new EntityNotFoundException(
                    "Invoice not found with id: " + payment.getInvoiceId()
                );
            });

        // Fetch customer from invoice
        Customer customer = customerRepository.findById(invoice.getCustomerId())
            .orElseThrow(() -> {
                logger.error("Customer not found for invoice: {}, customerId: {}",
                    invoice.getId(), invoice.getCustomerId());
                return new EntityNotFoundException(
                    "Customer not found with id: " + invoice.getCustomerId()
                );
            });

        // Map to response DTO with all computed fields
        PaymentResponseDTO response = PaymentMapper.toResponseDTO(payment, invoice, customer);

        logger.info("Retrieved payment {}", query.paymentId());

        return response;
    }
}
