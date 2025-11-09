package com.invoiceme.customer.api;

import com.invoiceme.customer.commands.*;
import com.invoiceme.customer.domain.Customer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Customer command operations (write side)
 * Handles create, update, and delete operations
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerCommandController {

    private final CreateCustomerCommandHandler createCustomerCommandHandler;
    private final UpdateCustomerCommandHandler updateCustomerCommandHandler;
    private final DeleteCustomerCommandHandler deleteCustomerCommandHandler;

    /**
     * Creates a new customer
     * POST /api/customers
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerCommand command) {
        try {
            Customer customer = createCustomerCommandHandler.handle(command);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CustomerResponse.from(customer));
        } catch (IllegalArgumentException e) {
            // Email already exists - return 409 Conflict
            throw new CustomerConflictException(e.getMessage());
        }
    }

    /**
     * Updates an existing customer
     * PUT /api/customers/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateCustomerRequest request
    ) {
        try {
            UpdateCustomerCommand command = new UpdateCustomerCommand(
                id,
                request.name(),
                request.email(),
                request.phone(),
                request.address()
            );
            Customer customer = updateCustomerCommandHandler.handle(command);
            return ResponseEntity.ok(CustomerResponse.from(customer));
        } catch (IllegalArgumentException e) {
            // Check if it's a not found or conflict error
            if (e.getMessage().contains("not found")) {
                throw new CustomerNotFoundException(e.getMessage());
            } else {
                throw new CustomerConflictException(e.getMessage());
            }
        }
    }

    /**
     * Soft-deletes a customer
     * DELETE /api/customers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        try {
            DeleteCustomerCommand command = new DeleteCustomerCommand(id);
            deleteCustomerCommandHandler.handle(command);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // Customer not found or already deleted
            throw new CustomerNotFoundException(e.getMessage());
        }
    }

    /**
     * Request DTO for updating customer
     */
    public record UpdateCustomerRequest(
        String name,
        String email,
        String phone,
        UpdateCustomerCommand.AddressDto address
    ) {}

    /**
     * Response DTO for customer operations
     */
    public record CustomerResponse(
        UUID id,
        String name,
        String email,
        String phone,
        AddressResponse address,
        String createdAt,
        String updatedAt
    ) {
        public record AddressResponse(
            String street,
            String city,
            String state,
            String postalCode,
            String country
        ) {}

        public static CustomerResponse from(Customer customer) {
            AddressResponse address = null;
            if (customer.getAddress() != null) {
                address = new AddressResponse(
                    customer.getAddress().getStreet(),
                    customer.getAddress().getCity(),
                    customer.getAddress().getState(),
                    customer.getAddress().getPostalCode(),
                    customer.getAddress().getCountry()
                );
            }

            return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                address,
                customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : null,
                customer.getUpdatedAt() != null ? customer.getUpdatedAt().toString() : null
            );
        }
    }

    /**
     * Exception for customer not found scenarios (404)
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class CustomerNotFoundException extends RuntimeException {
        public CustomerNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception for customer conflict scenarios (409)
     */
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class CustomerConflictException extends RuntimeException {
        public CustomerConflictException(String message) {
            super(message);
        }
    }
}
