package com.invoiceme.invoice.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.InvoiceMapper;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.commands.dto.LineItemDTO;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.domain.LineItem;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoiceme.invoice.domain.exceptions.InvoiceValidationException;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Handler for UpdateInvoiceCommand
 * Implements business logic for updating an invoice
 */
@Service
@RequiredArgsConstructor
public class UpdateInvoiceCommandHandler {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    /**
     * Handles invoice update command
     * @param command The update invoice command
     * @return InvoiceResponseDTO with updated invoice details
     * @throws InvoiceNotFoundException if invoice not found
     * @throws InvoiceValidationException if validation fails
     * @throws OptimisticLockException if version mismatch
     */
    @CacheEvict(value = {"dashboardStats"}, allEntries = true)
    @Transactional
    public InvoiceResponseDTO handle(UpdateInvoiceCommand command) {
        // Load existing invoice
        Invoice invoice = invoiceRepository.findById(command.invoiceId())
            .orElseThrow(() -> new InvoiceNotFoundException(command.invoiceId()));

        // Validate invoice status (can only update Draft invoices)
        if (invoice.getStatus() != InvoiceStatus.Draft) {
            throw new InvoiceValidationException(
                "Cannot update invoice in " + invoice.getStatus() + " status. Only Draft invoices can be updated.");
        }

        // Validate optimistic lock version
        if (!invoice.getVersion().equals(command.version())) {
            throw new OptimisticLockException(
                "Invoice has been modified by another transaction. Please refresh and try again.");
        }

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

        // Determine due date (use issueDate if not provided)
        LocalDate dueDate = command.dueDate() != null ? command.dueDate() : command.issueDate();

        // Update invoice fields
        invoice.updateFields(
            command.customerId(),
            command.issueDate(),
            dueDate,
            command.paymentTerms(),
            command.notes()
        );

        // Replace line items
        invoice.clearLineItems();
        for (LineItemDTO lineItemDTO : command.lineItems()) {
            LineItem lineItem = InvoiceMapper.toLineItemDomain(lineItemDTO);
            invoice.addLineItem(lineItem);
        }

        // Save updated invoice (JPA will automatically increment version)
        Invoice savedInvoice = invoiceRepository.save(invoice);

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
