package com.invoiceme.customer.api;

import com.invoiceme.customer.queries.GetCustomerByIdQuery;
import com.invoiceme.customer.queries.GetCustomerByIdQueryHandler;
import com.invoiceme.customer.queries.ListCustomersQuery;
import com.invoiceme.customer.queries.ListCustomersQueryHandler;
import com.invoiceme.customer.queries.dto.CustomerListItemDTO;
import com.invoiceme.customer.queries.dto.CustomerResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Customer Query operations (CQRS Read Side)
 * Handles GET requests for retrieving customer data
 */
@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer Queries", description = "Customer read operations (CQRS Query side)")
@SecurityRequirement(name = "bearerAuth")
public class CustomerQueryController {

    private final GetCustomerByIdQueryHandler getCustomerByIdQueryHandler;
    private final ListCustomersQueryHandler listCustomersQueryHandler;

    public CustomerQueryController(
        GetCustomerByIdQueryHandler getCustomerByIdQueryHandler,
        ListCustomersQueryHandler listCustomersQueryHandler
    ) {
        this.getCustomerByIdQueryHandler = getCustomerByIdQueryHandler;
        this.listCustomersQueryHandler = listCustomersQueryHandler;
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get customer by ID",
        description = "Retrieves detailed customer information including computed fields (totalInvoices, outstandingBalance)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Customer found",
            content = @Content(schema = @Schema(implementation = CustomerResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Customer not found",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token",
            content = @Content
        )
    })
    public ResponseEntity<CustomerResponseDTO> getCustomerById(
        @Parameter(description = "Customer UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID id
    ) {
        GetCustomerByIdQuery query = new GetCustomerByIdQuery(id);
        CustomerResponseDTO response = getCustomerByIdQueryHandler.handle(query);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
        summary = "List customers",
        description = "Retrieves paginated list of customers with optional filtering and sorting"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Customers retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token",
            content = @Content
        )
    })
    public ResponseEntity<Page<CustomerListItemDTO>> listCustomers(
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size (max 100)", example = "20")
        @RequestParam(defaultValue = "20") int size,

        @Parameter(description = "Sort field (name, email, createdAt)", example = "name")
        @RequestParam(defaultValue = "name") String sort,

        @Parameter(description = "Sort direction (asc, desc)", example = "asc")
        @RequestParam(defaultValue = "asc") String direction,

        @Parameter(description = "Search term for name or email", example = "john")
        @RequestParam(required = false) String search
    ) {
        ListCustomersQuery query = new ListCustomersQuery(page, size, sort, direction, search);
        Page<CustomerListItemDTO> response = listCustomersQueryHandler.handle(query);
        return ResponseEntity.ok(response);
    }
}
