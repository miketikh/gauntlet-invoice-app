package com.invoiceme.invoice.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.InvoiceMapper;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for GetInvoiceByIdQuery
 * Retrieves full invoice details with customer information and calculated fields
 */
@Service
public class GetInvoiceByIdQueryHandler {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public GetInvoiceByIdQueryHandler(InvoiceRepository invoiceRepository,
                                      CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Handles the query to get invoice by ID
     * @param query The query containing invoice ID
     * @return InvoiceResponseDTO with full details
     * @throws InvoiceNotFoundException if invoice not found
     */
    @Transactional(readOnly = true)
    public InvoiceResponseDTO handle(GetInvoiceByIdQuery query) {
        // Load invoice by ID
        Invoice invoice = invoiceRepository.findById(query.invoiceId())
            .orElseThrow(() -> new InvoiceNotFoundException(query.invoiceId()));

        // Load customer details
        Customer customer = customerRepository.findById(invoice.getCustomerId())
            .orElseThrow(() -> new RuntimeException("Customer not found for invoice: " + query.invoiceId()));

        // Convert to DTO with all computed fields
        return InvoiceMapper.toInvoiceResponseDTO(invoice, customer);
    }
}
