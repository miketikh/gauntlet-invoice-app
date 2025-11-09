package com.invoiceme.invoice.api;

import com.invoiceme.invoice.commands.*;
import com.invoiceme.invoice.commands.dto.CreateInvoiceDTO;
import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;
import com.invoiceme.invoice.commands.dto.UpdateInvoiceDTO;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import com.invoiceme.invoice.domain.exceptions.InvoiceValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for Invoice command operations (write side)
 * Handles create, update, and send invoice operations
 */
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoice Commands", description = "Invoice write operations")
public class InvoiceCommandController {

    private final CreateInvoiceCommandHandler createInvoiceCommandHandler;
    private final UpdateInvoiceCommandHandler updateInvoiceCommandHandler;
    private final SendInvoiceCommandHandler sendInvoiceCommandHandler;

    /**
     * Creates a new invoice
     * POST /api/invoices
     */
    @PostMapping
    @Operation(summary = "Create a new invoice", description = "Creates a new invoice in Draft status")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invoice created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<InvoiceResponseDTO> createInvoice(
        @Valid @RequestBody CreateInvoiceDTO dto
    ) {
        CreateInvoiceCommand command = new CreateInvoiceCommand(
            dto.customerId(),
            dto.issueDate(),
            dto.dueDate(),
            dto.paymentTerms(),
            dto.lineItems(),
            dto.notes()
        );

        InvoiceResponseDTO response = createInvoiceCommandHandler.handle(command);

        // Create Location header
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(response);
    }

    /**
     * Updates an existing invoice
     * PUT /api/invoices/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an invoice", description = "Updates an invoice (only Draft status allowed)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or invoice not in Draft status"),
        @ApiResponse(responseCode = "404", description = "Invoice not found"),
        @ApiResponse(responseCode = "409", description = "Optimistic lock version mismatch")
    })
    public ResponseEntity<InvoiceResponseDTO> updateInvoice(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInvoiceDTO dto
    ) {
        UpdateInvoiceCommand command = new UpdateInvoiceCommand(
            id,
            dto.customerId(),
            dto.issueDate(),
            dto.dueDate(),
            dto.paymentTerms(),
            dto.lineItems(),
            dto.notes(),
            dto.version()
        );

        InvoiceResponseDTO response = updateInvoiceCommandHandler.handle(command);
        return ResponseEntity.ok(response);
    }

    /**
     * Sends an invoice (transitions from Draft to Sent)
     * POST /api/invoices/{id}/send
     */
    @PostMapping("/{id}/send")
    @Operation(summary = "Send an invoice", description = "Sends an invoice (transitions from Draft to Sent)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invoice cannot be sent (no line items or wrong status)"),
        @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceResponseDTO> sendInvoice(@PathVariable UUID id) {
        SendInvoiceCommand command = new SendInvoiceCommand(id);
        InvoiceResponseDTO response = sendInvoiceCommandHandler.handle(command);
        return ResponseEntity.ok(response);
    }

    /**
     * Exception handler for InvoiceNotFoundException
     */
    @ExceptionHandler(InvoiceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleInvoiceNotFound(InvoiceNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    /**
     * Exception handler for InvoiceValidationException
     */
    @ExceptionHandler(InvoiceValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvoiceValidation(InvoiceValidationException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    /**
     * Exception handler for OptimisticLockException
     */
    @ExceptionHandler(OptimisticLockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLock(OptimisticLockException ex) {
        return new ErrorResponse("Invoice has been modified by another transaction. Please refresh and try again.");
    }

    /**
     * Error response DTO
     */
    public record ErrorResponse(String message) {}
}
