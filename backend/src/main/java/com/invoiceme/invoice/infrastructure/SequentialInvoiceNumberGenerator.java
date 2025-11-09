package com.invoiceme.invoice.infrastructure;

import com.invoiceme.invoice.domain.InvoiceNumberGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

/**
 * SequentialInvoiceNumberGenerator
 * Generates sequential invoice numbers using database sequence
 */
@Service
public class SequentialInvoiceNumberGenerator implements InvoiceNumberGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Generates the next invoice number using PostgreSQL function
     * @return Invoice number in format INV-{YEAR}-{SEQUENCE}
     */
    @Override
    @Transactional
    public String generateNextInvoiceNumber() {
        Query query = entityManager.createNativeQuery("SELECT generate_invoice_number()");
        String invoiceNumber = (String) query.getSingleResult();
        return invoiceNumber;
    }
}
