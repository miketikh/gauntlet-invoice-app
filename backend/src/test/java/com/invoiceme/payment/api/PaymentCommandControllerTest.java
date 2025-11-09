package com.invoiceme.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceme.payment.commands.RecordPaymentCommand;
import com.invoiceme.payment.commands.RecordPaymentCommandHandler;
import com.invoiceme.payment.commands.RecordPaymentDTO;
import com.invoiceme.payment.domain.PaymentMethod;
import com.invoiceme.payment.exceptions.InvoiceNotSentException;
import com.invoiceme.payment.exceptions.PaymentExceedsBalanceException;
import com.invoiceme.payment.queries.PaymentResponseDTO;
import com.invoiceme.invoice.domain.InvoiceStatus;
import com.invoiceme.invoice.domain.exceptions.InvoiceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for PaymentCommandController
 * Tests REST endpoint behavior and response formats
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PaymentCommandController API Tests")
class PaymentCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecordPaymentCommandHandler handler;

    @Test
    @WithMockUser(username = "user@example.com")
    @DisplayName("POST /api/invoices/{id}/payments should return 201 Created")
    void shouldRecordPaymentSuccessfully() throws Exception {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        LocalDate paymentDate = LocalDate.now();

        RecordPaymentDTO requestDto = new RecordPaymentDTO(
            paymentDate,
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            "REF-123",
            "Test payment",
            null
        );

        PaymentResponseDTO responseDTO = new PaymentResponseDTO(
            paymentId,
            invoiceId,
            paymentDate,
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            "REF-123",
            "Test payment",
            LocalDateTime.now(),
            "user@example.com",
            "INV-2024-0001",
            new BigDecimal("500.00"),
            new BigDecimal("400.00"),
            InvoiceStatus.Sent,
            "Acme Corp",
            "billing@acme.com",
            null
        );

        when(handler.handle(any(RecordPaymentCommand.class), eq("user@example.com")))
            .thenReturn(responseDTO);

        // When/Then
        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(paymentId.toString()))
            .andExpect(jsonPath("$.amount").value(100.00))
            .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
            .andExpect(jsonPath("$.reference").value("REF-123"))
            .andExpect(jsonPath("$.remainingBalance").value(400.00))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-2024-0001"))
            .andExpect(jsonPath("$.customerName").value("Acme Corp"))
            .andExpect(jsonPath("$.createdBy").value("user@example.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST with invalid payload should return 400 Bad Request")
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        // Given: Invalid payload with negative amount
        UUID invoiceId = UUID.randomUUID();
        RecordPaymentDTO invalidDto = new RecordPaymentDTO(
            LocalDate.now(),
            new BigDecimal("-50.00"),  // Invalid: negative amount
            PaymentMethod.CREDIT_CARD,
            null,
            null,
            null
        );

        // When/Then
        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Error"))
            .andExpect(jsonPath("$.errors").exists())
            .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST with missing required fields should return 400 Bad Request")
    void shouldReturnBadRequestForMissingFields() throws Exception {
        // Given: Payload missing required fields
        UUID invoiceId = UUID.randomUUID();
        String invalidJson = """
            {
                "reference": "REF-123"
            }
            """;

        // When/Then
        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST with future payment date should return 400 Bad Request")
    void shouldReturnBadRequestForFuturePaymentDate() throws Exception {
        // Given: Future payment date
        UUID invoiceId = UUID.randomUUID();
        RecordPaymentDTO futureDto = new RecordPaymentDTO(
            LocalDate.now().plusDays(1),  // Invalid: future date
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            null,
            null,
            null
        );

        // When/Then
        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(futureDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Error"))
            .andExpect(jsonPath("$.errors.paymentDate").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST with non-existent invoice should return 404 Not Found")
    void shouldReturnNotFoundForNonExistentInvoice() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        RecordPaymentDTO requestDto = new RecordPaymentDTO(
            LocalDate.now(),
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            null,
            null,
            null
        );

        when(handler.handle(any(RecordPaymentCommand.class), any()))
            .thenThrow(new InvoiceNotFoundException(nonExistentId));

        // When/Then
        mockMvc.perform(post("/api/invoices/{id}/payments", nonExistentId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Invoice Not Found"))
            .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST to draft invoice should return 400 Bad Request")
    void shouldReturnBadRequestForDraftInvoice() throws Exception {
        // Given
        UUID invoiceId = UUID.randomUUID();
        RecordPaymentDTO requestDto = new RecordPaymentDTO(
            LocalDate.now(),
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            null,
            null,
            null
        );

        when(handler.handle(any(RecordPaymentCommand.class), any()))
            .thenThrow(new InvoiceNotSentException(invoiceId, InvoiceStatus.Draft));

        // When/Then
        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid Invoice Status"))
            .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()))
            .andExpect(jsonPath("$.currentStatus").value("Draft"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST with payment exceeding balance should return 400 Bad Request")
    void shouldReturnBadRequestForPaymentExceedingBalance() throws Exception {
        // Given
        UUID invoiceId = UUID.randomUUID();
        RecordPaymentDTO requestDto = new RecordPaymentDTO(
            LocalDate.now(),
            new BigDecimal("600.00"),
            PaymentMethod.CREDIT_CARD,
            null,
            null,
            null
        );

        when(handler.handle(any(RecordPaymentCommand.class), any()))
            .thenThrow(new PaymentExceedsBalanceException(
                invoiceId,
                new BigDecimal("600.00"),
                new BigDecimal("500.00")
            ));

        // When/Then
        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Payment Amount Invalid"))
            .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()))
            .andExpect(jsonPath("$.paymentAmount").value(600.00))
            .andExpect(jsonPath("$.invoiceBalance").value(500.00));
    }

    @Test
    @DisplayName("POST without authentication should return 403 Forbidden (with CSRF) or 401")
    void shouldReturnUnauthorizedWithoutAuthentication() throws Exception {
        // Given
        UUID invoiceId = UUID.randomUUID();
        RecordPaymentDTO requestDto = new RecordPaymentDTO(
            LocalDate.now(),
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            null,
            null,
            null
        );

        // When/Then: Request without authentication returns 403 with CSRF enabled
        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("POST with idempotency key should be handled correctly")
    void shouldHandleIdempotencyKey() throws Exception {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "payment-test-001";

        RecordPaymentDTO requestDto = new RecordPaymentDTO(
            LocalDate.now(),
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            "REF-123",
            null,
            idempotencyKey
        );

        PaymentResponseDTO responseDTO = new PaymentResponseDTO(
            paymentId,
            invoiceId,
            LocalDate.now(),
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            "REF-123",
            null,
            LocalDateTime.now(),
            "user@example.com",
            "INV-2024-0001",
            new BigDecimal("500.00"),
            new BigDecimal("400.00"),
            InvoiceStatus.Sent,
            "Acme Corp",
            "billing@acme.com",
            null
        );

        when(handler.handle(any(RecordPaymentCommand.class), any()))
            .thenReturn(responseDTO);

        // When/Then
        mockMvc.perform(post("/api/invoices/{id}/payments", invoiceId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(paymentId.toString()));
    }
}
