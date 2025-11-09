package com.invoiceme.customer.commands;

import java.util.UUID;

/**
 * Command to soft-delete a customer
 */
public record DeleteCustomerCommand(
    UUID customerId
) {
}
