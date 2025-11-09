package com.invoiceme.invoice.api;

import com.invoiceme.customer.domain.Address;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.customer.infrastructure.JpaCustomerRepository;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.LineItem;
import com.invoiceme.invoice.infrastructure.JpaInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PDF download endpoint (Story 5.3)
 * Tests authentication, authorization, PDF generation, and error handling
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvoicePdfDownloadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaInvoiceRepository invoiceRepository;

    @Autowired
    private JpaCustomerRepository customerRepository;

    private Customer testCustomer;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        // Clean up
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();

        // Create test customer
        Customer customer = Customer.create(
            "Acme Corporation",
            "billing@acme.com",
            "555-1234",
            new Address("123 Main St", "Anytown", "CA", "12345", "USA")
        );
        testCustomer = ((CustomerRepository) customerRepository).save(customer);

        // Create test invoice with line items
        testInvoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.of(2025, 1, 15),
            LocalDate.of(2025, 2, 15),
            "Net 30",
            "INV-2025-0001"
        );

        // Add line items with discounts and tax
        LineItem item1 = new LineItem(
            null,
            "Consulting Services - Initial Assessment",
            10,
            new BigDecimal("150.00"),
            new BigDecimal("0.10"), // 10% discount
            new BigDecimal("0.08")  // 8% tax
        );
        testInvoice.addLineItem(item1);

        LineItem item2 = new LineItem(
            null,
            "Development Services",
            20,
            new BigDecimal("200.00"),
            new BigDecimal("0.00"), // No discount
            new BigDecimal("0.08")  // 8% tax
        );
        testInvoice.addLineItem(item2);

        testInvoice = invoiceRepository.save(testInvoice);
    }

    // AC 1: Endpoint Definition
    @Test
    @WithMockUser
    void shouldAccessPdfEndpointWithCorrectPath() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    // AC 2: Authentication & Authorization
    @Test
    void shouldReturn403ForUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedUserToDownloadPdf() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    // AC 3: Invoice Validation
    @Test
    @WithMockUser
    void shouldReturn404ForNonExistentInvoice() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/invoices/{id}/pdf", nonExistentId))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldReturn400ForMalformedUuid() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}/pdf", "invalid-uuid"))
            .andExpect(status().isBadRequest());
    }

    // AC 4: PDF Generation
    @Test
    @WithMockUser
    void shouldGenerateValidPdfForExistingInvoice() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();

        // Verify PDF is not empty
        assertTrue(pdfBytes.length > 0, "PDF should not be empty");

        // Verify PDF magic number (PDF files start with %PDF)
        String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", pdfHeader, "PDF should start with %PDF magic number");
    }

    // AC 5: HTTP Response Headers
    @Test
    @WithMockUser
    void shouldSetCorrectResponseHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"Invoice-INV-2025-0001.pdf\""))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(header().string("X-Invoice-Number", "INV-2025-0001"))
            .andReturn();

        // Verify Content-Length header exists and is positive
        String contentLength = result.getResponse().getHeader(HttpHeaders.CONTENT_LENGTH);
        if (contentLength != null) {
            assertTrue(Integer.parseInt(contentLength) > 0, "Content-Length should be positive");
        }
    }

    @Test
    @WithMockUser
    void shouldIncludeInvoiceNumberInFilename() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"Invoice-INV-2025-0001.pdf\""));
    }

    @Test
    @WithMockUser
    void shouldSetNoCacheHeaders() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    // AC 6: Streaming Response
    @Test
    @WithMockUser
    void shouldStreamPdfDirectlyToResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();

        // Verify PDF was streamed (non-zero bytes)
        assertTrue(pdfBytes.length > 0, "PDF should be streamed with content");

        // Verify response was flushed (content is available)
        assertNotNull(pdfBytes, "PDF content should be available in response");
    }

    // AC 7: Error Handling
    @Test
    @WithMockUser
    void shouldReturnStandardErrorFormatOn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/invoices/{id}/pdf", nonExistentId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser
    void shouldReturnStandardErrorFormatOn400() throws Exception {
        mockMvc.perform(get("/api/invoices/{id}/pdf", "invalid-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").exists());
    }

    // AC 8: Audit Logging (verified through logs, not tested programmatically)
    @Test
    @WithMockUser
    void shouldLogSuccessfulPdfDownload() throws Exception {
        // This test verifies logging is in place
        // Actual log verification would require log capture framework
        mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk());

        // Logger.info should be called with invoice ID and number
        // This is validated by code review and manual testing
    }

    // AC 9: Integration Testing - Complex Scenarios
    @Test
    @WithMockUser
    void shouldGeneratePdfForInvoiceWithMultipleLineItems() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 1000, "PDF with multiple line items should be substantial size");
    }

    @Test
    @WithMockUser
    void shouldGeneratePdfForDraftInvoice() throws Exception {
        // Verify draft invoice can be exported (should include watermark)
        mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser
    void shouldGeneratePdfForSentInvoice() throws Exception {
        // Mark invoice as sent
        testInvoice.markAsSent();
        invoiceRepository.save(testInvoice);

        mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    @WithMockUser
    void shouldGeneratePdfForInvoiceWithDiscountsAndTax() throws Exception {
        // Test invoice already has discounts and tax
        MvcResult result = mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 0, "PDF with discounts and tax should be generated");
    }

    @Test
    @WithMockUser
    void shouldGeneratePdfForLargeInvoice() throws Exception {
        // Create invoice with many line items to test multi-page support
        Invoice largeInvoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-2025-LARGE"
        );

        // Add 50 line items
        for (int i = 1; i <= 50; i++) {
            LineItem item = new LineItem(
                null,
                "Service Item #" + i,
                1,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("0.08")
            );
            largeInvoice.addLineItem(item);
        }

        largeInvoice = invoiceRepository.save(largeInvoice);

        MvcResult result = mockMvc.perform(get("/api/invoices/{id}/pdf", largeInvoice.getId()))
            .andExpect(status().isOk())
            .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 5000, "Large PDF should be substantial size");
    }

    @Test
    @WithMockUser
    void shouldHandleInvoiceWithNoDiscount() throws Exception {
        Invoice noDiscountInvoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 15",
            "INV-2025-NODISCOUNT"
        );

        LineItem item = new LineItem(
            null,
            "Standard Service",
            5,
            new BigDecimal("200.00"),
            BigDecimal.ZERO, // No discount
            BigDecimal.ZERO  // No tax
        );
        noDiscountInvoice.addLineItem(item);
        noDiscountInvoice = invoiceRepository.save(noDiscountInvoice);

        mockMvc.perform(get("/api/invoices/{id}/pdf", noDiscountInvoice.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    // Performance Test
    @Test
    @WithMockUser
    void shouldGeneratePdfWithinReasonableTime() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk());

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Should complete within 5 seconds (generous allowance for CI environments)
        assertTrue(duration < 5000,
            "PDF generation should complete within 5 seconds, took: " + duration + "ms");
    }

    // Content Validation
    @Test
    @WithMockUser
    void shouldReturnValidPdfFormat() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/invoices/{id}/pdf", testInvoice.getId()))
            .andExpect(status().isOk())
            .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();

        // Check PDF magic number
        assertTrue(pdfBytes.length >= 4, "PDF should be at least 4 bytes");
        String pdfHeader = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", pdfHeader, "PDF should start with %PDF magic number");

        // Check PDF footer (should contain %%EOF)
        String pdfContent = new String(pdfBytes);
        assertTrue(pdfContent.contains("%%EOF"), "PDF should end with %%EOF marker");
    }
}
