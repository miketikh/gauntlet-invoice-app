package com.invoiceme.invoice.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceme.customer.domain.Address;
import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.CreateInvoiceDTO;
import com.invoiceme.invoice.commands.dto.LineItemDTO;
import com.invoiceme.invoice.commands.dto.UpdateInvoiceDTO;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.infrastructure.JpaInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceCommandIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaInvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;
    private LineItemDTO testLineItem;

    @BeforeEach
    void setUp() {
        // Create a test customer
        testCustomer = Customer.create(
            "Test Customer",
            "test" + UUID.randomUUID() + "@example.com",
            "555-1234",
            new Address("123 Main St", "Test City", "TS", "12345", "USA")
        );
        testCustomer = customerRepository.save(testCustomer);

        // Create a test line item
        testLineItem = new LineItemDTO(
            "Consulting Services",
            10,
            new BigDecimal("100.00"),
            new BigDecimal("0.1"),
            new BigDecimal("0.08")
        );
    }

    @Test
    void shouldCreateInvoiceViaAPI() throws Exception {
        // Arrange
        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(30);

        CreateInvoiceDTO dto = new CreateInvoiceDTO(
            testCustomer.getId(),
            issueDate,
            dueDate,
            "Net 30",
            List.of(testLineItem),
            "Test invoice notes"
        );

        String json = objectMapper.writeValueAsString(dto);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.invoiceNumber").exists())
            .andExpect(jsonPath("$.status").value("Draft"))
            .andExpect(jsonPath("$.customerId").value(testCustomer.getId().toString()))
            .andExpect(jsonPath("$.customerName").value("Test Customer"))
            .andExpect(jsonPath("$.paymentTerms").value("Net 30"))
            .andExpect(jsonPath("$.lineItems").isArray())
            .andExpect(jsonPath("$.lineItems[0].description").value("Consulting Services"))
            .andExpect(jsonPath("$.lineItems[0].quantity").value(10))
            .andReturn();

        // Verify invoice was saved to database
        String responseJson = result.getResponse().getContentAsString();
        UUID invoiceId = UUID.fromString(
            objectMapper.readTree(responseJson).get("id").asText()
        );

        Invoice savedInvoice = ((JpaInvoiceRepository) invoiceRepository).findById(invoiceId).orElseThrow();
        assertThat(savedInvoice.getStatus()).isEqualTo(InvoiceStatus.Draft);
        assertThat(savedInvoice.getLineItems()).hasSize(1);
    }

    @Test
    void shouldReturnBadRequestWhenCreatingInvoiceWithEmptyLineItems() throws Exception {
        // Arrange
        CreateInvoiceDTO dto = new CreateInvoiceDTO(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(),  // Empty line items
            null
        );

        String json = objectMapper.writeValueAsString(dto);

        // Act & Assert
        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenCreatingInvoiceWithNonExistentCustomer() throws Exception {
        // Arrange
        CreateInvoiceDTO dto = new CreateInvoiceDTO(
            UUID.randomUUID(),  // Non-existent customer
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(testLineItem),
            null
        );

        String json = objectMapper.writeValueAsString(dto);

        // Act & Assert
        mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Customer not found")));
    }

    @Test
    void shouldUpdateDraftInvoice() throws Exception {
        // Arrange - Create an invoice first
        Invoice invoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-TEST-001"
        );
        invoice.addLineItem(new com.invoiceme.invoice.domain.LineItem(
            null, "Original Service", 1, new BigDecimal("50.00"),
            BigDecimal.ZERO, BigDecimal.ZERO
        ));
        invoice = ((JpaInvoiceRepository) invoiceRepository).save(invoice);

        // Update DTO
        LineItemDTO updatedLineItem = new LineItemDTO(
            "Updated Service",
            5,
            new BigDecimal("200.00"),
            new BigDecimal("0.05"),
            new BigDecimal("0.08")
        );

        UpdateInvoiceDTO dto = new UpdateInvoiceDTO(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(45),
            "Net 45",
            List.of(updatedLineItem),
            "Updated notes",
            invoice.getVersion()
        );

        String json = objectMapper.writeValueAsString(dto);

        // Act & Assert
        mockMvc.perform(put("/api/invoices/" + invoice.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentTerms").value("Net 45"))
            .andExpect(jsonPath("$.notes").value("Updated notes"))
            .andExpect(jsonPath("$.lineItems[0].description").value("Updated Service"));
    }

    @Test
    void shouldNotUpdateSentInvoice() throws Exception {
        // Arrange - Create and send an invoice
        Invoice invoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-TEST-002"
        );
        invoice.addLineItem(new com.invoiceme.invoice.domain.LineItem(
            null, "Service", 1, new BigDecimal("100.00"),
            BigDecimal.ZERO, BigDecimal.ZERO
        ));
        invoice.markAsSent();  // Send the invoice
        invoice = ((JpaInvoiceRepository) invoiceRepository).save(invoice);

        // Try to update
        UpdateInvoiceDTO dto = new UpdateInvoiceDTO(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(45),
            "Net 45",
            List.of(testLineItem),
            "Should fail",
            invoice.getVersion()
        );

        String json = objectMapper.writeValueAsString(dto);

        // Act & Assert
        mockMvc.perform(put("/api/invoices/" + invoice.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot update invoice")));
    }

    @Test
    void shouldReturnConflictWhenVersionMismatch() throws Exception {
        // Arrange - Create an invoice
        Invoice invoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-TEST-003"
        );
        invoice.addLineItem(new com.invoiceme.invoice.domain.LineItem(
            null, "Service", 1, new BigDecimal("100.00"),
            BigDecimal.ZERO, BigDecimal.ZERO
        ));
        invoice = ((JpaInvoiceRepository) invoiceRepository).save(invoice);

        // Try to update with wrong version
        UpdateInvoiceDTO dto = new UpdateInvoiceDTO(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(45),
            "Net 45",
            List.of(testLineItem),
            "Should fail",
            999L  // Wrong version
        );

        String json = objectMapper.writeValueAsString(dto);

        // Act & Assert
        mockMvc.perform(put("/api/invoices/" + invoice.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldSendDraftInvoice() throws Exception {
        // Arrange - Create a draft invoice
        Invoice invoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-TEST-004"
        );
        invoice.addLineItem(new com.invoiceme.invoice.domain.LineItem(
            null, "Service", 1, new BigDecimal("100.00"),
            BigDecimal.ZERO, BigDecimal.ZERO
        ));
        invoice = ((JpaInvoiceRepository) invoiceRepository).save(invoice);

        // Act & Assert
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/send"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Sent"));

        // Verify in database
        Invoice sentInvoice = ((JpaInvoiceRepository) invoiceRepository).findById(invoice.getId()).orElseThrow();
        assertThat(sentInvoice.getStatus()).isEqualTo(InvoiceStatus.Sent);
    }

    @Test
    void shouldNotSendInvoiceWithoutLineItems() throws Exception {
        // Arrange - Create invoice without line items
        Invoice invoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-TEST-005"
        );
        invoice = ((JpaInvoiceRepository) invoiceRepository).save(invoice);

        // Act & Assert
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/send"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("without line items")));
    }

    @Test
    void shouldNotSendAlreadySentInvoice() throws Exception {
        // Arrange - Create and send invoice
        Invoice invoice = Invoice.create(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            "INV-TEST-006"
        );
        invoice.addLineItem(new com.invoiceme.invoice.domain.LineItem(
            null, "Service", 1, new BigDecimal("100.00"),
            BigDecimal.ZERO, BigDecimal.ZERO
        ));
        invoice.markAsSent();
        invoice = ((JpaInvoiceRepository) invoiceRepository).save(invoice);

        // Act & Assert
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/send"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot send invoice")));
    }

    @Test
    void shouldCompleteFullWorkflow_CreateUpdateSend() throws Exception {
        // 1. Create invoice
        CreateInvoiceDTO createDto = new CreateInvoiceDTO(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(30),
            "Net 30",
            List.of(testLineItem),
            "Initial invoice"
        );

        MvcResult createResult = mockMvc.perform(post("/api/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        UUID invoiceId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());
        Long version = objectMapper.readTree(createResponse).get("version").asLong();

        // 2. Update invoice
        LineItemDTO updatedItem = new LineItemDTO(
            "Updated Consulting",
            15,
            new BigDecimal("150.00"),
            new BigDecimal("0.1"),
            new BigDecimal("0.08")
        );

        UpdateInvoiceDTO updateDto = new UpdateInvoiceDTO(
            testCustomer.getId(),
            LocalDate.now(),
            LocalDate.now().plusDays(45),
            "Net 45",
            List.of(updatedItem),
            "Updated invoice",
            version
        );

        mockMvc.perform(put("/api/invoices/" + invoiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentTerms").value("Net 45"));

        // 3. Send invoice
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/send"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Sent"));

        // 4. Verify final state
        Invoice finalInvoice = ((JpaInvoiceRepository) invoiceRepository).findById(invoiceId).orElseThrow();
        assertThat(finalInvoice.getStatus()).isEqualTo(InvoiceStatus.Sent);
        assertThat(finalInvoice.getPaymentTerms()).isEqualTo("Net 45");
        assertThat(finalInvoice.getLineItems()).hasSize(1);
        assertThat(finalInvoice.getLineItems().get(0).description()).isEqualTo("Updated Consulting");
    }
}
