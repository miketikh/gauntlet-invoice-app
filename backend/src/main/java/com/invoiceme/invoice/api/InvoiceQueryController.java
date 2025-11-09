package com.invoiceme.invoice.api;

import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.queries.GetInvoiceByIdQuery;
import com.invoiceme.invoice.queries.GetInvoiceByIdQueryHandler;
import com.invoiceme.invoice.queries.ListInvoicesQuery;
import com.invoiceme.invoice.queries.ListInvoicesQueryHandler;
import com.invoiceme.invoice.queries.dto.InvoiceListItemDTO;
import com.invoiceme.invoice.queries.dto.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST Controller for Invoice Query operations (CQRS Read side)
 * Handles GET requests for invoice retrieval
 */
@RestController
@RequestMapping("/invoices")
@Tag(name = "Invoice Queries", description = "Invoice retrieval and listing operations")
@SecurityRequirement(name = "bearer-jwt")
public class InvoiceQueryController {

    private final GetInvoiceByIdQueryHandler getInvoiceByIdQueryHandler;
    private final ListInvoicesQueryHandler listInvoicesQueryHandler;

    public InvoiceQueryController(
        GetInvoiceByIdQueryHandler getInvoiceByIdQueryHandler,
        ListInvoicesQueryHandler listInvoicesQueryHandler
    ) {
        this.getInvoiceByIdQueryHandler = getInvoiceByIdQueryHandler;
        this.listInvoicesQueryHandler = listInvoicesQueryHandler;
    }

    /**
     * Get invoice by ID with full details
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get invoice by ID",
        description = "Retrieves full invoice details including line items, customer info, and calculated fields"
    )
    public ResponseEntity<InvoiceResponseDTO> getInvoiceById(
        @Parameter(description = "Invoice ID") @PathVariable UUID id
    ) {
        GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(id);
        InvoiceResponseDTO response = getInvoiceByIdQueryHandler.handle(query);
        return ResponseEntity.ok(response);
    }

    /**
     * List invoices with optional filters and pagination
     */
    @GetMapping
    @Operation(
        summary = "List invoices",
        description = "Retrieves paginated list of invoices with optional filtering by customer, status, and date range"
    )
    public ResponseEntity<PagedResult<InvoiceListItemDTO>> listInvoices(
        @Parameter(description = "Filter by customer ID")
        @RequestParam(required = false) UUID customerId,

        @Parameter(description = "Filter by invoice status (Draft, Sent, Paid)")
        @RequestParam(required = false) InvoiceStatus status,

        @Parameter(description = "Filter by issue date >= startDate (format: yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

        @Parameter(description = "Filter by issue date <= endDate (format: yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") int size,

        @Parameter(description = "Sort field (invoiceNumber, issueDate, dueDate, status, totalAmount)")
        @RequestParam(defaultValue = "issueDate") String sortBy,

        @Parameter(description = "Sort direction (ASC or DESC)")
        @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        ListInvoicesQuery query = new ListInvoicesQuery(
            customerId,
            status,
            startDate,
            endDate,
            page,
            size,
            sortBy,
            sortDirection
        );

        PagedResult<InvoiceListItemDTO> response = listInvoicesQueryHandler.handle(query);
        return ResponseEntity.ok(response);
    }
}
