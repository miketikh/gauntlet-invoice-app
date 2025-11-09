package com.invoiceme.payment.api;

import com.invoiceme.payment.commands.RecordPaymentCommand;
import com.invoiceme.payment.commands.RecordPaymentCommandHandler;
import com.invoiceme.payment.commands.RecordPaymentDTO;
import com.invoiceme.payment.queries.PaymentResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * PaymentCommandController
 * REST API controller for payment command operations
 * Handles recording payments against invoices
 */
@RestController
@RequestMapping("/invoices")
@Tag(name = "Payment Commands", description = "Operations for recording payments against invoices")
public class PaymentCommandController {

    private final RecordPaymentCommandHandler recordPaymentHandler;

    public PaymentCommandController(RecordPaymentCommandHandler recordPaymentHandler) {
        this.recordPaymentHandler = recordPaymentHandler;
    }

    /**
     * Records a payment against an invoice
     *
     * @param id The invoice ID to apply payment to
     * @param dto The payment details
     * @param authentication The authenticated user
     * @return PaymentResponseDTO with payment and updated invoice information
     */
    @PostMapping("/{id}/payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Record payment against invoice",
        description = """
            Creates a payment record and updates invoice balance.
            If balance reaches zero, marks invoice as Paid.
            Supports idempotency via optional idempotencyKey field.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Payment recorded successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponseDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or business rule violation",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Invoice not found",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required",
            content = @Content(mediaType = "application/json")
        )
    })
    public ResponseEntity<PaymentResponseDTO> recordPayment(
        @Parameter(description = "Invoice ID", required = true)
        @PathVariable UUID id,

        @Parameter(
            description = "Payment details",
            required = true,
            schema = @Schema(implementation = RecordPaymentDTO.class)
        )
        @RequestBody @Valid RecordPaymentDTO dto,

        Authentication authentication
    ) {
        // Get current user from authentication
        String userId = authentication.getName();

        // Create command from DTO
        RecordPaymentCommand command = new RecordPaymentCommand(
            id,
            dto.paymentDate(),
            dto.amount(),
            dto.paymentMethod(),
            dto.reference(),
            dto.notes(),
            dto.idempotencyKey()
        );

        // Handle command
        PaymentResponseDTO response = recordPaymentHandler.handle(command, userId);

        // Return 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
