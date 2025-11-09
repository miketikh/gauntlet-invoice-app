package com.invoiceme.invoice.infrastructure;

import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Custom repository implementation for complex invoice queries
 * Uses JPA Criteria API for dynamic filtering
 */
@Repository
public class InvoiceRepositoryCustomImpl implements InvoiceRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Invoice> findWithFilters(
        UUID customerId,
        InvoiceStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Query for results
        CriteriaQuery<Invoice> query = cb.createQuery(Invoice.class);
        Root<Invoice> invoice = query.from(Invoice.class);

        // Build predicates dynamically
        List<Predicate> predicates = buildPredicates(cb, invoice, customerId, status, startDate, endDate);

        // Apply predicates
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // Apply sorting
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(cb.asc(invoice.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(invoice.get(order.getProperty())));
                }
            });
            query.orderBy(orders);
        }

        // Execute query with pagination
        TypedQuery<Invoice> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Invoice> results = typedQuery.getResultList();

        // Count query for total elements
        long total = countWithFilters(customerId, status, startDate, endDate);

        return new PageImpl<>(results, pageable, total);
    }

    /**
     * Builds dynamic predicates based on provided filters
     */
    private List<Predicate> buildPredicates(
        CriteriaBuilder cb,
        Root<Invoice> invoice,
        UUID customerId,
        InvoiceStatus status,
        LocalDate startDate,
        LocalDate endDate
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (customerId != null) {
            predicates.add(cb.equal(invoice.get("customerId"), customerId));
        }

        if (status != null) {
            predicates.add(cb.equal(invoice.get("status"), status));
        }

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(invoice.get("issueDate"), startDate));
        }

        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(invoice.get("issueDate"), endDate));
        }

        return predicates;
    }

    /**
     * Counts total invoices matching filters
     */
    private long countWithFilters(
        UUID customerId,
        InvoiceStatus status,
        LocalDate startDate,
        LocalDate endDate
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Invoice> invoice = countQuery.from(Invoice.class);

        countQuery.select(cb.count(invoice));

        List<Predicate> predicates = buildPredicates(cb, invoice, customerId, status, startDate, endDate);

        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    @Override
    public long countByStatus(InvoiceStatus status) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Invoice> invoice = query.from(Invoice.class);

        query.select(cb.count(invoice))
            .where(cb.equal(invoice.get("status"), status));

        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public BigDecimal calculateTotalRevenue() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<Invoice> invoice = query.from(Invoice.class);

        query.select(cb.sum(invoice.get("totalAmount")))
            .where(cb.equal(invoice.get("status"), InvoiceStatus.Paid));

        BigDecimal result = entityManager.createQuery(query).getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateOutstandingAmount() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<Invoice> invoice = query.from(Invoice.class);

        query.select(cb.sum(invoice.get("balance")))
            .where(cb.equal(invoice.get("status"), InvoiceStatus.Sent));

        BigDecimal result = entityManager.createQuery(query).getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateOverdueAmount() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<Invoice> invoice = query.from(Invoice.class);

        query.select(cb.sum(invoice.get("balance")))
            .where(cb.and(
                cb.equal(invoice.get("status"), InvoiceStatus.Sent),
                cb.lessThan(invoice.get("dueDate"), LocalDate.now())
            ));

        BigDecimal result = entityManager.createQuery(query).getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }
}
