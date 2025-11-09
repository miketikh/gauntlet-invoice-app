package com.invoiceme.customer.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.customer.domain.events.CustomerDeleted;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for DeleteCustomerCommand
 * Implements soft-delete logic for customers
 */
@Service
@RequiredArgsConstructor
public class DeleteCustomerCommandHandler {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handles customer deletion command (soft delete)
     * @param command The delete customer command
     * @throws IllegalArgumentException if customer not found
     */
    @Transactional
    public void handle(DeleteCustomerCommand command) {
        // Find customer
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Customer not found with id: " + command.customerId()
            ));

        // Check if already deleted
        if (customer.isDeleted()) {
            throw new IllegalArgumentException(
                "Customer already deleted with id: " + command.customerId()
            );
        }

        // TODO: Check for dependent entities (invoices, payments) when those modules are implemented
        // This will be implemented in future stories when invoice and payment domains are added

        // Soft delete customer
        customer.delete();

        // Save customer with deletedAt timestamp set
        customerRepository.save(customer);

        // Publish domain event
        eventPublisher.publishEvent(
            new CustomerDeleted(customer.getId())
        );
    }
}
