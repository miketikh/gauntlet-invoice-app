package com.invoiceme.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentRepository
 * Repository interface for Payment entity following the repository pattern
 * Extends JpaSpecificationExecutor for dynamic query support
 * Spring Data JPA will provide implementation automatically
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    /**
     * Find all payments for a specific invoice ordered by payment date descending
     * Uses index on invoice_id for performance
     *
     * @param invoiceId The invoice ID to search for
     * @return List of payments for the invoice (empty list if none)
     */
    @Query("SELECT p FROM Payment p WHERE p.invoiceId = :invoiceId ORDER BY p.paymentDate DESC")
    List<Payment> findByInvoiceId(@Param("invoiceId") UUID invoiceId);

    /**
     * Find all payments for a specific invoice ordered by payment date ascending (chronological)
     * Used for running balance calculations
     *
     * @param invoiceId The invoice ID to search for
     * @return List of payments for the invoice in chronological order (empty list if none)
     */
    @Query("SELECT p FROM Payment p WHERE p.invoiceId = :invoiceId ORDER BY p.paymentDate ASC, p.createdAt ASC")
    List<Payment> findByInvoiceIdOrderByPaymentDateAsc(@Param("invoiceId") UUID invoiceId);

    /**
     * Find a specific payment by ID and invoice ID
     * Useful for verifying payment belongs to specific invoice
     *
     * @param id The payment ID
     * @param invoiceId The invoice ID
     * @return Optional containing payment if found
     */
    Optional<Payment> findByIdAndInvoiceId(UUID id, UUID invoiceId);

    /**
     * Find payments within a date range
     * Useful for payment reports and reconciliation
     *
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return List of payments in the date range
     */
    List<Payment> findByPaymentDateBetween(LocalDate start, LocalDate end);

    /**
     * Find payments by payment method
     * Useful for payment method analysis and reporting
     *
     * @param method The payment method to filter by
     * @return List of payments using the specified method
     */
    List<Payment> findByPaymentMethod(PaymentMethod method);

    /**
     * Calculate total amount collected from all payments
     *
     * @return Total amount collected (BigDecimal)
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p")
    BigDecimal calculateTotalCollected();

    /**
     * Calculate total amount collected within a date range
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Total amount collected in date range
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalCollectedInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Calculate total amount collected for a specific date
     *
     * @param date The date to calculate for
     * @return Total amount collected on that date
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentDate = :date")
    BigDecimal calculateTotalCollectedOnDate(@Param("date") LocalDate date);

    /**
     * Count total number of payments
     *
     * @return Total payment count
     */
    @Query("SELECT COUNT(p) FROM Payment p")
    long countTotalPayments();
}
