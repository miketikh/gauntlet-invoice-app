package com.invoiceme.customer.domain;

import java.util.UUID;

/**
 * Exception thrown when a customer is not found
 */
public class CustomerNotFoundException extends RuntimeException {

    private final UUID customerId;

    public CustomerNotFoundException(UUID customerId) {
        super("Customer not found with id: " + customerId);
        this.customerId = customerId;
    }

    public UUID getCustomerId() {
        return customerId;
    }
}
