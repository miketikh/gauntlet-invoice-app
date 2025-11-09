package com.invoiceme.builders;

import com.invoiceme.invoice.commands.dto.CreateInvoiceDTO;
import com.invoiceme.invoice.commands.dto.LineItemDTO;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceRepository;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.domain.LineItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Builder for Invoice test data
 */
public class InvoiceTestBuilder {

    private UUID customerId = UUID.randomUUID();
    private LocalDate issueDate = LocalDate.now();
    private LocalDate dueDate = LocalDate.now().plusDays(30);
    private String paymentTerms = "Net 30";
    private String invoiceNumber = "INV-TEST-001";
    private InvoiceStatus status = InvoiceStatus.Draft;
    private List<LineItem> lineItems = new ArrayList<>();
    private String notes = null;

    private InvoiceTestBuilder() {
        // Add default line item
        lineItems.add(LineItemTestBuilder.create().build());
    }

    public static InvoiceTestBuilder create() {
        return new InvoiceTestBuilder();
    }

    public InvoiceTestBuilder withCustomerId(UUID customerId) {
        this.customerId = customerId;
        return this;
    }

    public InvoiceTestBuilder withIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
        return this;
    }

    public InvoiceTestBuilder withDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public InvoiceTestBuilder withPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
        return this;
    }

    public InvoiceTestBuilder withInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
        return this;
    }

    public InvoiceTestBuilder withStatus(InvoiceStatus status) {
        this.status = status;
        return this;
    }

    public InvoiceTestBuilder withLineItem(LineItem lineItem) {
        if (this.lineItems.isEmpty() || this.lineItems.get(0).description().equals("Test Line Item")) {
            // Replace default line item with provided one
            this.lineItems.clear();
        }
        this.lineItems.add(lineItem);
        return this;
    }

    public InvoiceTestBuilder withLineItems(List<LineItem> lineItems) {
        this.lineItems = new ArrayList<>(lineItems);
        return this;
    }

    public InvoiceTestBuilder withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public InvoiceTestBuilder withDefaults() {
        this.customerId = UUID.randomUUID();
        this.issueDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(30);
        this.paymentTerms = "Net 30";
        this.invoiceNumber = "INV-TEST-001";
        this.status = InvoiceStatus.Draft;
        this.lineItems = new ArrayList<>();
        this.lineItems.add(LineItemTestBuilder.create().build());
        this.notes = null;
        return this;
    }

    /**
     * Build Invoice entity
     */
    public Invoice build() {
        Invoice invoice = Invoice.create(customerId, issueDate, dueDate, paymentTerms, invoiceNumber);

        // Add line items
        for (LineItem lineItem : lineItems) {
            invoice.addLineItem(lineItem);
        }

        // Set notes if provided
        if (notes != null) {
            invoice.setNotes(notes);
        }

        return invoice;
    }

    /**
     * Build CreateInvoiceDTO for API requests
     */
    public CreateInvoiceDTO buildDTO() {
        List<LineItemDTO> lineItemDTOs = lineItems.stream()
            .map(li -> new LineItemDTO(
                li.description(),
                li.quantity(),
                li.unitPrice(),
                li.discountPercent(),
                li.taxRate()
            ))
            .toList();

        // If no line items provided, use default
        if (lineItemDTOs.isEmpty()) {
            lineItemDTOs = List.of(LineItemTestBuilder.create().buildDTO());
        }

        return new CreateInvoiceDTO(
            customerId,
            issueDate,
            dueDate,
            paymentTerms,
            lineItemDTOs,
            notes
        );
    }

    /**
     * Build and save Invoice to repository
     */
    public Invoice buildAndSave(InvoiceRepository repository) {
        Invoice invoice = build();
        return repository.save(invoice);
    }

    /**
     * Build multiple Invoice instances
     */
    public List<Invoice> buildList(int count, UUID customerId) {
        List<Invoice> invoices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            invoices.add(
                InvoiceTestBuilder.create()
                    .withCustomerId(customerId)
                    .withInvoiceNumber("INV-TEST-" + String.format("%03d", i + 1))
                    .build()
            );
        }
        return invoices;
    }

    /**
     * Build and save multiple Invoice instances
     */
    public List<Invoice> buildAndSaveList(com.invoiceme.invoice.infrastructure.JpaInvoiceRepository repository, int count, UUID customerId) {
        List<Invoice> invoices = buildList(count, customerId);
        return repository.saveAll(invoices);
    }
}
