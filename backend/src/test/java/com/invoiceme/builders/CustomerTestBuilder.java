package com.invoiceme.builders;

import com.invoiceme.customer.commands.CreateCustomerCommand;
import com.invoiceme.customer.domain.Address;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Customer test data
 * Follows the builder pattern for flexible test data creation
 */
public class CustomerTestBuilder {

    private String name = "Test Customer";
    private String email = "test@example.com";
    private String phone = "555-0100";
    private Address address = new Address(
        "123 Test St",
        "Test City",
        "Test State",
        "12345",
        "Test Country"
    );

    private CustomerTestBuilder() {
    }

    public static CustomerTestBuilder create() {
        return new CustomerTestBuilder();
    }

    public CustomerTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public CustomerTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public CustomerTestBuilder withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public CustomerTestBuilder withAddress(Address address) {
        this.address = address;
        return this;
    }

    public CustomerTestBuilder withAddress(String street, String city, String state, String postalCode, String country) {
        this.address = new Address(street, city, state, postalCode, country);
        return this;
    }

    public CustomerTestBuilder withDefaults() {
        this.name = "Test Customer";
        this.email = "test@example.com";
        this.phone = "555-0100";
        this.address = new Address("123 Test St", "Test City", "Test State", "12345", "Test Country");
        return this;
    }

    /**
     * Build Customer entity
     */
    public Customer build() {
        return Customer.create(name, email, phone, address);
    }

    /**
     * Build CreateCustomerCommand for API requests
     */
    public CreateCustomerCommand buildCommand() {
        CreateCustomerCommand.AddressDto addressDto = address != null
            ? new CreateCustomerCommand.AddressDto(
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
            )
            : null;

        return new CreateCustomerCommand(name, email, phone, addressDto);
    }

    /**
     * Build and save Customer to repository
     */
    public Customer buildAndSave(CustomerRepository repository) {
        Customer customer = build();
        return repository.save(customer);
    }

    /**
     * Build multiple Customer instances
     */
    public List<Customer> buildList(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            customers.add(
                CustomerTestBuilder.create()
                    .withName("Customer " + i)
                    .withEmail("customer" + i + "@example.com")
                    .build()
            );
        }
        return customers;
    }

    /**
     * Build and save multiple Customer instances
     */
    public List<Customer> buildAndSaveList(com.invoiceme.customer.infrastructure.JpaCustomerRepository repository, int count) {
        List<Customer> customers = buildList(count);
        return repository.saveAll(customers);
    }
}
