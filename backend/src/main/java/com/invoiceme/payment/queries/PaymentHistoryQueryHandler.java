package com.invoiceme.payment.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.payment.domain.Payment;
import com.invoiceme.payment.domain.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PaymentHistoryQueryHandler
 * Handler for retrieving paginated payment history with dynamic filtering
 */
@Service
public class PaymentHistoryQueryHandler {

    private static final Logger logger = LoggerFactory.getLogger(PaymentHistoryQueryHandler.class);

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public PaymentHistoryQueryHandler(
        PaymentRepository paymentRepository,
        InvoiceRepository invoiceRepository,
        CustomerRepository customerRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Handles the PaymentHistoryQuery
     * Returns paginated payment history with optional filters
     *
     * @param query The query with optional filters and pagination
     * @return Page of PaymentResponseDTO
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponseDTO> handle(PaymentHistoryQuery query) {
        logger.debug("Payment history query: customerId={}, dateRange={}-{}, method={}, page={}",
            query.customerId().orElse(null),
            query.startDate().orElse(null),
            query.endDate().orElse(null),
            query.paymentMethod().orElse(null),
            query.pageable().getPageNumber()
        );

        // Build dynamic specification based on Optional fields
        Specification<Payment> spec = buildSpecification(query);

        // Apply pagination and sorting (default: paymentDate DESC)
        var pageable = query.pageable();
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "paymentDate")
            );
        }

        // Fetch paginated payments
        Page<Payment> paymentsPage = paymentRepository.findAll(spec, pageable);

        // Convert to DTOs with enriched data
        List<PaymentResponseDTO> dtos = enrichPaymentsWithInvoiceAndCustomerData(paymentsPage.getContent());

        logger.info("Payment history query returned {} results (total: {})",
            dtos.size(), paymentsPage.getTotalElements());

        return new PageImpl<>(dtos, pageable, paymentsPage.getTotalElements());
    }

    /**
     * Builds dynamic specification from query filters
     *
     * @param query The query with optional filters
     * @return Combined Specification
     */
    private Specification<Payment> buildSpecification(PaymentHistoryQuery query) {
        Specification<Payment> spec = Specification.where(null);

        // Add customer filter if present
        if (query.customerId().isPresent()) {
            spec = spec.and(PaymentSpecification.byCustomerId(query.customerId().get()));
        }

        // Add date range filter if present
        if (query.startDate().isPresent() || query.endDate().isPresent()) {
            spec = spec.and(PaymentSpecification.byDateRange(
                query.startDate().orElse(null),
                query.endDate().orElse(null)
            ));
        }

        // Add payment method filter if present
        if (query.paymentMethod().isPresent()) {
            spec = spec.and(PaymentSpecification.byPaymentMethod(query.paymentMethod().get()));
        }

        return spec;
    }

    /**
     * Enriches payments with invoice and customer data
     * Uses batching to avoid N+1 queries
     *
     * @param payments List of payment entities
     * @return List of enriched PaymentResponseDTOs
     */
    private List<PaymentResponseDTO> enrichPaymentsWithInvoiceAndCustomerData(List<Payment> payments) {
        if (payments.isEmpty()) {
            return List.of();
        }

        // Collect unique invoice IDs and load them
        Map<UUID, Invoice> invoiceMap = new HashMap<>();
        for (Payment payment : payments) {
            UUID invoiceId = payment.getInvoiceId();
            if (!invoiceMap.containsKey(invoiceId)) {
                invoiceRepository.findById(invoiceId)
                    .ifPresent(invoice -> invoiceMap.put(invoiceId, invoice));
            }
        }

        // Collect unique customer IDs and load them
        Map<UUID, Customer> customerMap = new HashMap<>();
        for (Invoice invoice : invoiceMap.values()) {
            UUID customerId = invoice.getCustomerId();
            if (!customerMap.containsKey(customerId)) {
                customerRepository.findById(customerId)
                    .ifPresent(customer -> customerMap.put(customerId, customer));
            }
        }

        // Map to DTOs
        return payments.stream()
            .map(payment -> {
                Invoice invoice = invoiceMap.get(payment.getInvoiceId());
                Customer customer = invoice != null ? customerMap.get(invoice.getCustomerId()) : null;

                if (invoice == null || customer == null) {
                    logger.warn("Missing invoice or customer data for payment: {}", payment.getId());
                    return null;
                }

                return PaymentMapper.toResponseDTO(payment, invoice, customer);
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }
}
