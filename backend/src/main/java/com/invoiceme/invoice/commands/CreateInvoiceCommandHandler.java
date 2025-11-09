package com.invoiceme.invoice.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.InvoiceMapper;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.commands.dto.LineItemDTO;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceNumberGenerator;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.invoice.domain.LineItem;
import com.invoiceme.invoice.domain.events.InvoiceCreated;
import com.invoiceme.invoice.domain.exceptions.InvoiceValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Handler for CreateInvoiceCommand
 * Implements business logic for creating a new invoice
 */
@Service
@RequiredArgsConstructor
public class CreateInvoiceCommandHandler {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handles invoice creation command
     * @param command The create invoice command
     * @return InvoiceResponseDTO with created invoice details
     * @throws InvoiceValidationException if validation fails
     */
    @Transactional
    public InvoiceResponseDTO handle(CreateInvoiceCommand command) {
        // Validate customer exists
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new InvoiceValidationException(
                "Customer not found: " + command.customerId()));

        // Validate line items
        if (command.lineItems() == null || command.lineItems().isEmpty()) {
            throw new InvoiceValidationException("At least one line item is required");
        }

        // Validate dates
        validateDates(command.issueDate(), command.dueDate());

        // Generate invoice number
        String invoiceNumber = invoiceNumberGenerator.generateNextInvoiceNumber();

        // Determine due date (use issueDate if not provided)
        LocalDate dueDate = command.dueDate() != null ? command.dueDate() : command.issueDate();

        // Create invoice aggregate
        Invoice invoice = Invoice.create(
            command.customerId(),
            command.issueDate(),
            dueDate,
            command.paymentTerms(),
            invoiceNumber
        );

        // Add line items
        for (LineItemDTO lineItemDTO : command.lineItems()) {
            LineItem lineItem = InvoiceMapper.toLineItemDomain(lineItemDTO);
            invoice.addLineItem(lineItem);
        }

        // Set notes if provided
        if (command.notes() != null) {
            invoice.setNotes(command.notes());
        }

        // Save invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Publish domain events
        savedInvoice.getDomainEvents().forEach(eventPublisher::publishEvent);

        // Return response DTO
        return InvoiceMapper.toInvoiceResponseDTO(savedInvoice, customer);
    }

    /**
     * Validates issue date and due date
     */
    private void validateDates(LocalDate issueDate, LocalDate dueDate) {
        if (issueDate == null) {
            throw new InvoiceValidationException("Issue date is required");
        }

        if (dueDate != null && dueDate.isBefore(issueDate)) {
            throw new InvoiceValidationException("Due date must be on or after issue date");
        }
    }
}
