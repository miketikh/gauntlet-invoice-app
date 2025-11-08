package com.invoiceme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for InvoiceMe ERP System
 * Implements DDD, CQRS, and Vertical Slice Architecture
 */
@SpringBootApplication
public class InvoiceMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceMeApplication.class, args);
    }
}