package com.invoiceme.payment.commands;

import com.invoiceme.common.idempotency.IdempotencyService;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoiceme.payment.queries.PaymentMapper;
import com.invoiceme.payment.domain.Payment;
import com.invoiceme.payment.domain.PaymentRepository;
import com.invoiceme.payment.domain.PaymentService;
import com.invoiceme.payment.domain.events.PaymentRecorded;
import com.invoiceme.payment.exceptions.InvoiceNotSentException;
import com.invoiceme.payment.exceptions.PaymentExceedsBalanceException;
import com.invoiceme.payment.queries.PaymentResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * RecordPaymentCommandHandler
 * Handles the recording of payments against invoices with transactional consistency
 *
 * This handler orchestrates:
 * 1. Idempotency checking (if key provided)
 * 2. Invoice validation and retrieval
 * 3. Payment creation and validation
 * 4. Invoice balance update
 * 5. Invoice status transition (if fully paid)
 * 6. Domain event publishing
 * 7. Response DTO creation
 *
 * All operations occur within a single transaction to ensure atomicity
 */
@Service
public class RecordPaymentCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(RecordPaymentCommandHandler.class);

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public RecordPaymentCommandHandler(
        PaymentRepository paymentRepository,
        InvoiceRepository invoiceRepository,
        CustomerRepository customerRepository,
        PaymentService paymentService,
        IdempotencyService idempotencyService
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Handles the RecordPaymentCommand
     *
     * @param command The command containing payment details
     * @param userId The ID of the user recording the payment
     * @return PaymentResponseDTO with payment and updated invoice information
     * @throws InvoiceNotFoundException if invoice not found
     * @throws InvoiceNotSentException if invoice is not in Sent status
     * @throws PaymentExceedsBalanceException if payment amount exceeds invoice balance
     */
    @CacheEvict(value = {"dashboardStats"}, allEntries = true)
    @Transactional
    public PaymentResponseDTO handle(RecordPaymentCommand command, String userId) {
        log.info("Recording payment for invoice {}, amount: {}, user: {}",
            command.invoiceId(), command.amount(), userId);

        try {
            // Step 1: Check idempotency (if key provided)
            if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
                Optional<PaymentResponseDTO> cachedResult = idempotencyService.checkIdempotency(
                    command.idempotencyKey(),
                    PaymentResponseDTO.class
                );
                if (cachedResult.isPresent()) {
                    log.info("Idempotency key found: {} - Returning cached response", command.idempotencyKey());
                    return cachedResult.get();
                }
            }

            // Step 2: Fetch invoice
            log.debug("Fetching invoice: {}", command.invoiceId());
            Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(command.invoiceId()));
            log.debug("Invoice found: id={}, status={}, balance={}",
                invoice.getId(), invoice.getStatus(), invoice.getBalance());

            // Step 3: Validate invoice status (must be Sent)
            if (!invoice.canAcceptPayment()) {
                log.warn("Payment validation failed: Invoice {} is in {} status, expected Sent",
                    command.invoiceId(), invoice.getStatus());
                throw new InvoiceNotSentException(command.invoiceId(), invoice.getStatus());
            }

            // Step 4: Validate payment amount doesn't exceed balance
            if (command.amount().compareTo(invoice.getBalance()) > 0) {
                log.warn("Payment validation failed: Payment amount ({}) exceeds invoice balance ({})",
                    command.amount(), invoice.getBalance());
                throw new PaymentExceedsBalanceException(
                    command.invoiceId(),
                    command.amount(),
                    invoice.getBalance()
                );
            }

            // Step 5: Create Payment entity
            Payment payment = Payment.createPayment(
                command.invoiceId(),
                command.paymentDate(),
                command.amount(),
                command.paymentMethod(),
                command.reference(),
                command.notes(),
                userId
            );

            // Step 6: Validate payment against invoice (cross-aggregate validation)
            paymentService.validatePaymentAgainstInvoice(payment, invoice);

            // Step 7: Apply payment to invoice (updates balance and auto-transitions to Paid if balance is zero)
            invoice.applyPayment(command.amount());

            // Log if invoice was marked as Paid
            if (invoice.getStatus() == com.invoiceme.invoice.domain.InvoiceStatus.Paid) {
                log.info("Invoice {} marked as Paid due to zero balance", command.invoiceId());
            }

            // Step 8: Save payment to database
            Payment savedPayment = paymentRepository.save(payment);

            // Step 9: Save updated invoice
            Invoice savedInvoice = invoiceRepository.save(invoice);

            // Step 10: Publish PaymentRecorded domain event
            PaymentRecorded event = PaymentRecorded.of(savedPayment, savedInvoice);
            log.info("PaymentRecorded event: paymentId={}, invoiceId={}, newBalance={}, newStatus={}",
                event.paymentId(), event.invoiceId(), event.newBalance(), event.newStatus());

            // Step 11: Fetch customer for enriched response
            Customer customer = customerRepository.findById(savedInvoice.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found for invoice: " + savedInvoice.getId()));

            // Step 12: Create response DTO
            PaymentResponseDTO response = PaymentMapper.toResponseDTO(
                savedPayment,
                savedInvoice,
                customer
            );

            // Step 13: Store idempotency record (if key provided)
            if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
                idempotencyService.storeIdempotency(command.idempotencyKey(), response);
            }

            log.info("Payment recorded successfully: paymentId={}, newBalance={}, newStatus={}",
                savedPayment.getId(), savedInvoice.getBalance(), savedInvoice.getStatus());

            return response;

        } catch (InvoiceNotFoundException | InvoiceNotSentException | PaymentExceedsBalanceException e) {
            // Expected business exceptions - let them bubble up to GlobalExceptionHandler
            throw e;
        } catch (Exception e) {
            // Unexpected exception - log with full stack trace
            log.error("Unexpected error recording payment for invoice {}: {} - {}",
                command.invoiceId(), e.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }
}
