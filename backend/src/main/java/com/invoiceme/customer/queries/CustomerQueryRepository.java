package com.invoiceme.customer.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.infrastructure.JpaCustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Query repository for customer read operations
 * Extends JpaCustomerRepository with additional query methods for CQRS pattern
 * Note: No @Repository annotation needed - Spring will create a bean from JpaCustomerRepository
 */
public interface CustomerQueryRepository extends JpaCustomerRepository {

    /**
     * Finds all non-deleted customers with pagination
     */
    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL")
    Page<Customer> findAllNotDeleted(Pageable pageable);

    /**
     * Searches customers by name or email with pagination
     * Excludes soft-deleted customers
     */
    @Query("SELECT c FROM Customer c WHERE c.deletedAt IS NULL AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> searchByNameOrEmail(@Param("search") String search, Pageable pageable);

    /**
     * Counts total invoices for a customer
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.customerId = :customerId")
    Integer countInvoicesByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Calculates outstanding balance for a customer (sum of unpaid invoice balances)
     */
    @Query("SELECT COALESCE(SUM(i.balance), 0) FROM Invoice i WHERE i.customerId = :customerId AND i.balance > 0")
    BigDecimal calculateOutstandingBalance(@Param("customerId") UUID customerId);
}
