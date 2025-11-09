package com.invoiceme.payment.queries;

import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.payment.domain.Payment;
import com.invoiceme.payment.domain.PaymentMethod;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

/**
 * PaymentSpecification
 * Utility class for building dynamic query specifications for filtering payments
 */
public class PaymentSpecification {

    private PaymentSpecification() {
        // Private constructor to prevent instantiation
    }

    /**
     * Specification to filter payments by customer ID
     * Uses subquery to join payment -> invoice -> customer
     *
     * @param customerId The customer ID to filter by
     * @return Specification for customer filter
     */
    public static Specification<Payment> byCustomerId(UUID customerId) {
        return (root, query, criteriaBuilder) -> {
            if (customerId == null) {
                return criteriaBuilder.conjunction();
            }

            // Create subquery to find invoices for the customer
            var subquery = query.subquery(UUID.class);
            var invoiceRoot = subquery.from(Invoice.class);
            subquery.select(invoiceRoot.get("id"))
                .where(criteriaBuilder.equal(invoiceRoot.get("customerId"), customerId));

            // Filter payments where invoiceId is in the customer's invoices
            return root.get("invoiceId").in(subquery);
        };
    }

    /**
     * Specification to filter payments by date range
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Specification for date range filter
     */
    public static Specification<Payment> byDateRange(LocalDate startDate, LocalDate endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return criteriaBuilder.conjunction();
            }

            if (startDate != null && endDate != null) {
                return criteriaBuilder.between(
                    root.get("paymentDate"),
                    startDate,
                    endDate
                );
            }

            if (startDate != null) {
                return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("paymentDate"),
                    startDate
                );
            }

            // endDate != null
            return criteriaBuilder.lessThanOrEqualTo(
                root.get("paymentDate"),
                endDate
            );
        };
    }

    /**
     * Specification to filter payments by payment method
     *
     * @param method The payment method to filter by
     * @return Specification for payment method filter
     */
    public static Specification<Payment> byPaymentMethod(PaymentMethod method) {
        return (root, query, criteriaBuilder) -> {
            if (method == null) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.equal(root.get("paymentMethod"), method);
        };
    }

}
