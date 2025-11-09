package com.invoiceme.customer.infrastructure;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of CustomerRepository
 * Extends Spring Data JPA repository and implements domain interface
 * Marked as @Primary to be used for command operations when CustomerRepository is injected
 */
@Repository
@Primary
public interface JpaCustomerRepository extends JpaRepository<Customer, UUID>, CustomerRepository {

    /**
     * Saves a customer (create or update)
     * Explicitly overrides both JpaRepository and CustomerRepository to resolve ambiguity
     */
    @Override
    Customer save(Customer customer);

    /**
     * Finds a customer by ID, excluding soft-deleted customers
     * Overrides default findById to filter out deleted customers
     */
    @Override
    @Query("SELECT c FROM Customer c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Customer> findById(@Param("id") UUID id);

    /**
     * Checks if an email exists for non-deleted customers
     */
    @Override
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE LOWER(c.email) = LOWER(:email) AND c.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Checks if an email exists for non-deleted customers, excluding specific customer
     */
    @Override
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE LOWER(c.email) = LOWER(:email) AND c.id != :excludeId AND c.deletedAt IS NULL")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("excludeId") UUID excludeCustomerId);
}
