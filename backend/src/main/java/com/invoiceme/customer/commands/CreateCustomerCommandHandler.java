package com.invoiceme.customer.commands;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.customer.domain.events.CustomerCreated;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for CreateCustomerCommand
 * Implements business logic for creating a new customer
 */
@Service
@RequiredArgsConstructor
public class CreateCustomerCommandHandler {

    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handles customer creation command
     * @param command The create customer command
     * @return Created customer
     * @throws IllegalArgumentException if email already exists
     */
    @Transactional
    public Customer handle(CreateCustomerCommand command) {
        // Validate email uniqueness
        if (customerRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("Email already exists: " + command.email());
        }

        // Create customer
        Customer customer = Customer.create(
            command.name(),
            command.email(),
            command.phone(),
            command.getAddressDomain()
        );

        // Save customer
        Customer savedCustomer = customerRepository.save(customer);

        // Publish domain event
        eventPublisher.publishEvent(
            new CustomerCreated(
                savedCustomer.getId(),
                savedCustomer.getName(),
                savedCustomer.getEmail()
            )
        );

        return savedCustomer;
    }
}
