package com.invoiceme.customer.queries;

import java.util.UUID;

/**
 * Query to get a single customer by ID
 */
public record GetCustomerByIdQuery(UUID customerId) {
    public GetCustomerByIdQuery {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
    }
}
