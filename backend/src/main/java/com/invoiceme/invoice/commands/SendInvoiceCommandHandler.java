package com.invoiceme.invoice.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.InvoiceMapper;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoiceme.invoice.domain.exceptions.InvoiceValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for SendInvoiceCommand
 * Implements business logic for sending an invoice
 */
@Service
@RequiredArgsConstructor
public class SendInvoiceCommandHandler {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handles send invoice command
     * @param command The send invoice command
     * @return InvoiceResponseDTO with updated invoice details
     * @throws InvoiceNotFoundException if invoice not found
     * @throws InvoiceValidationException if validation fails
     */
    @CacheEvict(value = {"dashboardStats"}, allEntries = true)
    @Transactional
    public InvoiceResponseDTO handle(SendInvoiceCommand command) {
        // Load invoice
        Invoice invoice = invoiceRepository.findById(command.invoiceId())
            .orElseThrow(() -> new InvoiceNotFoundException(command.invoiceId()));

        // Validate invoice status (can only send Draft invoices)
        if (invoice.getStatus() != InvoiceStatus.Draft) {
            throw new InvoiceValidationException(
                "Cannot send invoice in " + invoice.getStatus() + " status. Only Draft invoices can be sent.");
        }

        // Validate invoice has line items (markAsSent will also check this)
        if (!invoice.canBeSent()) {
            throw new InvoiceValidationException("Cannot send invoice without line items");
        }

        // Load customer for response
        Customer customer = customerRepository.findById(invoice.getCustomerId())
            .orElseThrow(() -> new InvoiceValidationException(
                "Customer not found: " + invoice.getCustomerId()));

        // Mark invoice as sent (domain method handles state transition)
        invoice.markAsSent();

        // Save updated invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Publish domain events
        savedInvoice.getDomainEvents().forEach(eventPublisher::publishEvent);

        // Return response DTO
        return InvoiceMapper.toInvoiceResponseDTO(savedInvoice, customer);
    }
}
