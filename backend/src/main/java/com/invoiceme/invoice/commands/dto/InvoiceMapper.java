package com.invoiceme.invoice.commands.dto;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.LineItem;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between Invoice domain objects and DTOs
 */
public class InvoiceMapper {

    private InvoiceMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts LineItemDTO to domain LineItem
     * @param dto The LineItemDTO
     * @return Domain LineItem
     */
    public static LineItem toLineItemDomain(LineItemDTO dto) {
        return new LineItem(
            null,  // ID will be generated
            dto.description(),
            dto.quantity(),
            dto.unitPrice(),
            dto.discountPercent(),
            dto.taxRate()
        );
    }

    /**
     * Converts domain LineItem to LineItemResponseDTO
     * @param lineItem Domain LineItem
     * @return LineItemResponseDTO
     */
    public static LineItemResponseDTO toLineItemResponseDTO(LineItem lineItem) {
        return new LineItemResponseDTO(
            lineItem.id(),
            lineItem.description(),
            lineItem.quantity(),
            lineItem.unitPrice(),
            lineItem.discountPercent(),
            lineItem.taxRate(),
            lineItem.subtotal(),
            lineItem.discountAmount(),
            lineItem.taxableAmount(),
            lineItem.taxAmount(),
            lineItem.total()
        );
    }

    /**
     * Converts Invoice and Customer to InvoiceResponseDTO
     * @param invoice The invoice domain object
     * @param customer The customer domain object
     * @return InvoiceResponseDTO
     */
    public static InvoiceResponseDTO toInvoiceResponseDTO(Invoice invoice, Customer customer) {
        List<LineItemResponseDTO> lineItemDTOs = invoice.getLineItems().stream()
            .map(InvoiceMapper::toLineItemResponseDTO)
            .collect(Collectors.toList());

        Integer daysOverdue = calculateDaysOverdue(invoice);

        return new InvoiceResponseDTO(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getCustomerId(),
            customer.getName(),
            customer.getEmail(),
            invoice.getIssueDate(),
            invoice.getDueDate(),
            invoice.getStatus(),
            invoice.getPaymentTerms(),
            invoice.getSubtotal(),
            invoice.getTotalDiscount(),
            invoice.getTotalTax(),
            invoice.getTotalAmount(),
            invoice.getBalance(),
            lineItemDTOs,
            invoice.getNotes(),
            invoice.getVersion(),
            invoice.getCreatedAt(),
            invoice.getUpdatedAt(),
            daysOverdue
        );
    }

    /**
     * Calculates days overdue for an invoice
     * @param invoice The invoice
     * @return Number of days overdue, or null if not overdue or paid
     */
    private static Integer calculateDaysOverdue(Invoice invoice) {
        if (invoice.getStatus() == com.invoiceme.invoice.domain.InvoiceStatus.Paid) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = invoice.getDueDate();

        if (today.isAfter(dueDate)) {
            return (int) ChronoUnit.DAYS.between(dueDate, today);
        }

        return null;
    }
}
