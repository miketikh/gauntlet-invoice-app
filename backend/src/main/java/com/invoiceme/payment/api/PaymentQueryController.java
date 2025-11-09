package com.invoiceme.payment.api;

import com.invoiceme.payment.domain.PaymentMethod;
import com.invoiceme.payment.queries.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentQueryController
 * REST Controller for Payment Query operations (CQRS Read side)
 * Handles GET requests for payment retrieval and statistics
 */
@RestController
@RequestMapping("/payments")
@Tag(name = "Payment Queries", description = "Payment retrieval, history, and statistics operations")
@SecurityRequirement(name = "bearer-jwt")
public class PaymentQueryController {

    private final GetPaymentByIdQueryHandler getPaymentByIdQueryHandler;
    private final PaymentHistoryQueryHandler paymentHistoryQueryHandler;
    private final GetPaymentStatisticsQueryHandler getPaymentStatisticsQueryHandler;

    public PaymentQueryController(
        GetPaymentByIdQueryHandler getPaymentByIdQueryHandler,
        PaymentHistoryQueryHandler paymentHistoryQueryHandler,
        GetPaymentStatisticsQueryHandler getPaymentStatisticsQueryHandler
    ) {
        this.getPaymentByIdQueryHandler = getPaymentByIdQueryHandler;
        this.paymentHistoryQueryHandler = paymentHistoryQueryHandler;
        this.getPaymentStatisticsQueryHandler = getPaymentStatisticsQueryHandler;
    }

    /**
     * Get payment by ID with enriched invoice and customer data
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get payment by ID",
        description = "Retrieves payment details with invoice and customer information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment found and returned"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing authentication")
    })
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
        @Parameter(description = "Payment ID") @PathVariable UUID id
    ) {
        GetPaymentByIdQuery query = new GetPaymentByIdQuery(id);
        PaymentResponseDTO response = getPaymentByIdQueryHandler.handle(query);
        return ResponseEntity.ok(response);
    }

    /**
     * List payments with optional filters and pagination
     */
    @GetMapping
    @Operation(
        summary = "List payments with filters",
        description = "Retrieves paginated payment history with optional filters (customer, date range, payment method)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment list returned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing authentication")
    })
    public ResponseEntity<Page<PaymentResponseDTO>> listPayments(
        @Parameter(description = "Filter by customer ID")
        @RequestParam(required = false) UUID customerId,

        @Parameter(description = "Filter by payment date >= startDate (format: yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

        @Parameter(description = "Filter by payment date <= endDate (format: yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

        @Parameter(description = "Filter by payment method (CREDIT_CARD, BANK_TRANSFER, CHECK, CASH)")
        @RequestParam(required = false) PaymentMethod paymentMethod,

        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size (max 100)")
        @RequestParam(defaultValue = "20") int size,

        @Parameter(description = "Sort field (paymentDate, amount, createdAt)")
        @RequestParam(defaultValue = "paymentDate") String sortBy,

        @Parameter(description = "Sort direction (ASC or DESC)")
        @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        // Validate and limit page size
        if (size > 100) {
            size = 100;
        }

        // Create pageable with sorting
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Build query with optional filters
        PaymentHistoryQuery query = new PaymentHistoryQuery(
            Optional.ofNullable(customerId),
            Optional.ofNullable(startDate),
            Optional.ofNullable(endDate),
            Optional.ofNullable(paymentMethod),
            pageable
        );

        Page<PaymentResponseDTO> response = paymentHistoryQueryHandler.handle(query);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment statistics for dashboard
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get payment statistics",
        description = "Retrieves aggregated payment statistics for dashboard (total collected, by method, by period)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics calculated and returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing authentication")
    })
    public ResponseEntity<PaymentStatisticsDTO> getPaymentStatistics(
        @Parameter(description = "Optional start date for filtering statistics (format: yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

        @Parameter(description = "Optional end date for filtering statistics (format: yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        GetPaymentStatisticsQuery query = new GetPaymentStatisticsQuery(
            Optional.ofNullable(startDate),
            Optional.ofNullable(endDate)
        );

        PaymentStatisticsDTO response = getPaymentStatisticsQueryHandler.handle(query);
        return ResponseEntity.ok(response);
    }
}
