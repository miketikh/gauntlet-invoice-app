package com.invoiceme.customer.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.customer.domain.events.CustomerUpdated;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for UpdateCustomerCommand
 * Implements business logic for updating an existing customer
 */
@Service
@RequiredArgsConstructor
public class UpdateCustomerCommandHandler {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handles customer update command
     * @param command The update customer command
     * @return Updated customer
     * @throws IllegalArgumentException if customer not found or email conflict
     */
    @CacheEvict(value = {"customerList", "customers", "dashboardStats", "invoiceList"}, allEntries = true)
    @Transactional
    public Customer handle(UpdateCustomerCommand command) {
        // Find customer
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Customer not found with id: " + command.customerId()
            ));

        // Validate email uniqueness if email is being updated
        if (command.email() != null &&
            !command.email().equalsIgnoreCase(customer.getEmail())) {
            if (customerRepository.existsByEmailAndIdNot(command.email(), command.customerId())) {
                throw new IllegalArgumentException("Email already exists: " + command.email());
            }
        }

        // Update customer
        customer.update(
            command.name(),
            command.email(),
            command.phone(),
            command.getAddressDomain()
        );

        // Save customer
        Customer updatedCustomer = customerRepository.save(customer);

        // Publish domain event
        eventPublisher.publishEvent(
            new CustomerUpdated(
                updatedCustomer.getId(),
                updatedCustomer.getName(),
                updatedCustomer.getEmail()
            )
        );

        return updatedCustomer;
    }
}
