package com.invoiceme.invoice.api;

import com.invoiceme.common.services.InvoicePdfService;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.queries.GetInvoiceByIdQuery;
import com.invoiceme.invoice.queries.GetInvoiceByIdQueryHandler;
import com.invoiceme.invoice.queries.ListInvoicesQuery;
import com.invoiceme.invoice.queries.ListInvoicesQueryHandler;
import com.invoiceme.invoice.queries.dto.InvoiceListItemDTO;
import com.invoiceme.invoice.queries.dto.PagedResult;
import com.invoiceme.payment.queries.ListPaymentsByInvoiceQuery;
import com.invoiceme.payment.queries.ListPaymentsByInvoiceQueryHandler;
import com.invoiceme.payment.queries.PaymentResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
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

    private static final Logger logger = LoggerFactory.getLogger(InvoiceQueryController.class);

    private final GetInvoiceByIdQueryHandler getInvoiceByIdQueryHandler;
    private final ListInvoicesQueryHandler listInvoicesQueryHandler;
    private final ListPaymentsByInvoiceQueryHandler listPaymentsByInvoiceQueryHandler;
    private final InvoicePdfService invoicePdfService;

    public InvoiceQueryController(
        GetInvoiceByIdQueryHandler getInvoiceByIdQueryHandler,
        ListInvoicesQueryHandler listInvoicesQueryHandler,
        ListPaymentsByInvoiceQueryHandler listPaymentsByInvoiceQueryHandler,
        InvoicePdfService invoicePdfService
    ) {
        this.getInvoiceByIdQueryHandler = getInvoiceByIdQueryHandler;
        this.listInvoicesQueryHandler = listInvoicesQueryHandler;
        this.listPaymentsByInvoiceQueryHandler = listPaymentsByInvoiceQueryHandler;
        this.invoicePdfService = invoicePdfService;
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

    /**
     * Get all payments for a specific invoice with running balance
     */
    @GetMapping("/{id}/payments")
    @Operation(
        summary = "Get payments for invoice",
        description = "Retrieves all payments for specific invoice with running balance calculation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment list returned successfully"),
        @ApiResponse(responseCode = "404", description = "Invoice not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing authentication")
    })
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsForInvoice(
        @Parameter(description = "Invoice ID") @PathVariable UUID id
    ) {
        ListPaymentsByInvoiceQuery query = new ListPaymentsByInvoiceQuery(id);
        List<PaymentResponseDTO> response = listPaymentsByInvoiceQueryHandler.handle(query);
        return ResponseEntity.ok(response);
    }

    /**
     * Download invoice as PDF
     * Streams PDF directly to response for efficient memory usage
     */
    @GetMapping("/{id}/pdf")
    @Operation(
        summary = "Download invoice as PDF",
        description = "Generates and downloads a professional PDF document for the specified invoice"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "PDF generated and downloaded successfully",
            content = @Content(
                mediaType = "application/pdf",
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid invoice ID format"),
        @ApiResponse(responseCode = "404", description = "Invoice not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing authentication"),
        @ApiResponse(responseCode = "500", description = "PDF generation failed")
    })
    public void downloadInvoicePdf(
        @Parameter(description = "Invoice ID") @PathVariable UUID id,
        HttpServletResponse response
    ) throws IOException {
        logger.info("PDF download requested for invoice ID: {}", id);

        try {
            // Retrieve invoice using existing query handler
            GetInvoiceByIdQuery query = new GetInvoiceByIdQuery(id);
            InvoiceResponseDTO invoice = getInvoiceByIdQueryHandler.handle(query);

            // Set response headers for PDF download
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"Invoice-" + invoice.invoiceNumber() + ".pdf\""
            );
            response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
            response.setHeader("X-Invoice-Number", invoice.invoiceNumber());

            // Stream PDF directly to response output stream
            try (OutputStream outputStream = response.getOutputStream()) {
                invoicePdfService.generateInvoicePdf(invoice, outputStream);
                outputStream.flush();
            }

            logger.info("PDF successfully generated and streamed for invoice: {} (ID: {})",
                invoice.invoiceNumber(), id);

        } catch (Exception e) {
            logger.error("Failed to generate PDF for invoice ID: {}", id, e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
}
