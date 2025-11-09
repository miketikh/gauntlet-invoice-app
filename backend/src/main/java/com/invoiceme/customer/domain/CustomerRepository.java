package com.invoiceme.customer.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Customer aggregate
 * Domain layer interface - implementation in infrastructure layer
 */
public interface CustomerRepository {

    /**
     * Saves a customer (create or update)
     * @param customer Customer to save
     * @return Saved customer
     */
    Customer save(Customer customer);

    /**
     * Finds a customer by ID, excluding soft-deleted customers
     * @param id Customer ID
     * @return Optional containing customer if found and not deleted
     */
    Optional<Customer> findById(UUID id);

    /**
     * Checks if an email exists for non-deleted customers
     * @param email Email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks if an email exists for non-deleted customers, excluding specific customer
     * Used for update operations to allow keeping the same email
     * @param email Email to check
     * @param excludeCustomerId Customer ID to exclude from check
     * @return true if email exists for other customers
     */
    boolean existsByEmailAndIdNot(String email, UUID excludeCustomerId);
}
