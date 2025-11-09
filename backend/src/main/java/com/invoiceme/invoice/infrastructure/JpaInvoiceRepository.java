package com.invoiceme.invoice.infrastructure;

import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JpaInvoiceRepository
 * Spring Data JPA repository implementation for Invoice persistence
 * Extends both JpaRepository (for JPA functionality) and InvoiceRepository (domain interface)
 * Also extends InvoiceRepositoryCustom for complex queries
 */
@Repository
public interface JpaInvoiceRepository extends JpaRepository<Invoice, UUID>, InvoiceRepository, InvoiceRepositoryCustom {

    @Override
    @NonNull
    Invoice save(@NonNull Invoice invoice);

    @Override
    @NonNull
    Optional<Invoice> findById(@NonNull UUID id);
}
