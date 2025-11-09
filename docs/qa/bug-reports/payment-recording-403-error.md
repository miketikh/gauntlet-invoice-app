# Bug Report: Payment Recording Returns Misleading Error for Sent Invoices

**Bug ID**: BUG-2025-001
**Severity**: HIGH
**Priority**: P1 - Critical
**Status**: Open
**Reported By**: Quinn (QA Agent)
**Date Reported**: 2025-11-08
**Affected Stories**: 3.2 (Payment Recording Commands), 3.4 (Payment Recording UI)

---

## Executive Summary

Users receive a misleading error message "cannot record payment on a draft invoice, please send the invoice first" when attempting to record payment on invoices that are **already in Sent status**. This creates confusion and blocks legitimate payment recording operations.

## Reproduction Steps

1. Create a new invoice with line items
2. Mark the invoice as "Sent" (status persists correctly in database)
3. Navigate to the invoice detail page
4. Verify invoice shows status badge as "Sent" ✓
5. Click "Record Payment" button
6. Fill in payment form with valid data:
   - Amount: Any amount ≤ invoice balance
   - Payment method: Any valid method
   - Reference: Any non-empty string
7. Submit the payment form
8. **Observe**: Error toast displays "cannot record payment on a draft invoice, please send the invoice first"

## Expected Behavior

- Payment should be recorded successfully for Sent invoices
- Invoice balance should decrease by payment amount
- Success toast should display "Payment of $X.XX recorded successfully"
- If there IS an error, the error message should accurately reflect the actual problem

## Actual Behavior

- Payment fails with HTTP 403 Forbidden
- Frontend displays hardcoded error message about draft invoices
- Error message does not match the actual invoice state
- User is confused because invoice is clearly marked as "Sent"

## Environment

- **Invoice ID**: `30db4f6a-0349-4948-bda3-65058c110b8f`
- **Invoice Number**: INV-2025-0001
- **Invoice Status (DB)**: Sent ✓
- **Invoice Balance**: $90.00
- **Database**: PostgreSQL (verified via direct query)
- **Backend**: Spring Boot (running on port 8080)
- **Frontend**: Next.js (React)

## Technical Analysis

### Database State (Confirmed)
```sql
SELECT id, invoice_number, status, balance, total_amount
FROM invoices
WHERE id = '30db4f6a-0349-4948-bda3-65058c110b8f';

-- Result:
-- status: Sent ✓
-- balance: 90.00
-- total_amount: 90.00
```

### Server Logs (Problematic Pattern)
```
2025-11-08 22:58:06 - Set Authentication for user: admin ✓
2025-11-08 22:58:06 - Secured POST /invoices/{id}/payments ✓
2025-11-08 22:58:06 - Securing POST /error ← UNEXPECTED
2025-11-08 22:58:06 - Set SecurityContextHolder to anonymous
2025-11-08 22:58:06 - Pre-authenticated entry point called. Rejecting access (403)
```

**Analysis**: The request starts successfully with authentication, but then Spring redirects to `/error` endpoint, loses authentication context, and returns 403 Forbidden.

### Root Causes Identified

#### Bug #1: Incorrect Frontend Error Mapping (PRIMARY CAUSE)
**Location**: `lib/api/payments.ts:32-34`

```typescript
if (error.response?.status === 403) {
  throw new Error('Cannot record payment on a draft invoice. Please send the invoice first.');
}
```

**Problem**:
- Frontend incorrectly assumes HTTP 403 = "draft invoice"
- HTTP 403 is a generic authorization error with many possible causes
- Backend actually returns 400 (not 403) for `InvoiceNotSentException`
- This hardcoded message masks the actual backend error

**Impact**: Users receive incorrect diagnostic information, making troubleshooting impossible.

---

#### Bug #2: Unhandled Backend Exception Causing 403
**Location**: `backend/src/main/java/com/invoiceme/payment/commands/RecordPaymentCommandHandler.java`

**Evidence**:
- Server logs show redirect to `/error` endpoint during payment processing
- `/error` endpoint requires authentication but security context is lost
- Some unhandled exception is occurring in the payment flow
- `GlobalExceptionHandler` may not be catching this exception type

**Hypothesis**: Possible causes (requires investigation):
1. NullPointerException in payment processing
2. Database constraint violation
3. Serialization/deserialization error
4. Missing required field in DTO mapping
5. Transaction isolation issue

**Impact**: Legitimate payment requests fail with misleading 403 instead of proper error response.

---

## Recommended Fixes

### Fix #1: Correct Frontend Error Handling (IMMEDIATE - P0)

**File**: `lib/api/payments.ts`

**Current Code** (lines 26-34):
```typescript
if (error.response?.status === 400) {
  throw new Error(
    error.response.data?.message || 'Invalid payment data. Please check amount and date.'
  );
}
if (error.response?.status === 403) {
  throw new Error('Cannot record payment on a draft invoice. Please send the invoice first.');
}
if (error.response?.status === 404) {
  throw new Error('Invoice not found.');
}
```

**Recommended Fix**:
```typescript
if (error.response?.status === 400 || error.response?.status === 403) {
  // Extract the backend's actual error message from ProblemDetail
  const detail = error.response.data?.detail ||
                 error.response.data?.message ||
                 'Invalid payment request';

  // Include specific error properties if available
  const currentStatus = error.response.data?.currentStatus;
  const contextualMessage = currentStatus
    ? `${detail} (Invoice status: ${currentStatus})`
    : detail;

  throw new Error(contextualMessage);
}
if (error.response?.status === 404) {
  throw new Error('Invoice not found.');
}
```

**Rationale**:
- Trust backend's `ProblemDetail` response for accurate error messages
- Stop making assumptions about what HTTP codes mean
- Preserve contextual information from backend (invoice status, etc.)

---

### Fix #2: Add Comprehensive Error Logging (IMMEDIATE - P0)

**File**: `backend/src/main/java/com/invoiceme/payment/commands/RecordPaymentCommandHandler.java`

**Add try-catch around entire handle method**:
```java
@Transactional
public PaymentResponseDTO handle(RecordPaymentCommand command, String userId) {
    log.info("Recording payment for invoice {}, amount: {}, user: {}",
        command.invoiceId(), command.amount(), userId);

    try {
        // Step 1: Check idempotency (if key provided)
        if (command.idempotencyKey() != null && !command.idempotencyKey().isBlank()) {
            Optional<PaymentResponseDTO> cachedResult = idempotencyService.checkIdempotency(
                command.idempotencyKey(),
                PaymentResponseDTO.class
            );
            if (cachedResult.isPresent()) {
                log.info("Idempotency key found: {} - Returning cached response", command.idempotencyKey());
                return cachedResult.get();
            }
        }

        // Step 2: Fetch invoice
        log.debug("Fetching invoice: {}", command.invoiceId());
        Invoice invoice = invoiceRepository.findById(command.invoiceId())
            .orElseThrow(() -> new InvoiceNotFoundException(command.invoiceId()));
        log.debug("Invoice found: id={}, status={}, balance={}",
            invoice.getId(), invoice.getStatus(), invoice.getBalance());

        // Step 3: Validate invoice status (must be Sent)
        if (!invoice.canAcceptPayment()) {
            log.warn("Payment validation failed: Invoice {} is in {} status, expected Sent",
                command.invoiceId(), invoice.getStatus());
            throw new InvoiceNotSentException(command.invoiceId(), invoice.getStatus());
        }

        // ... rest of existing code ...

        log.info("Payment recorded successfully: paymentId={}, newBalance={}, newStatus={}",
            savedPayment.getId(), savedInvoice.getBalance(), savedInvoice.getStatus());

        return response;

    } catch (InvoiceNotFoundException | InvoiceNotSentException | PaymentExceedsBalanceException e) {
        // Expected business exceptions - let them bubble up to GlobalExceptionHandler
        throw e;
    } catch (Exception e) {
        // Unexpected exception - log with full stack trace
        log.error("Unexpected error recording payment for invoice {}: {} - {}",
            command.invoiceId(), e.getClass().getName(), e.getMessage(), e);
        throw e;
    }
}
```

**Rationale**:
- Capture ALL exceptions with full stack traces
- Distinguish between expected business exceptions and unexpected errors
- Provide detailed logging for debugging production issues

---

### Fix #3: Add Catch-All Exception Handler (HIGH PRIORITY)

**File**: `backend/src/main/java/com/invoiceme/config/GlobalExceptionHandler.java`

**Uncomment and improve lines 159-169**:
```java
/**
 * Handles all unhandled exceptions
 * Returns 500 Internal Server Error
 * This prevents Spring's default error page redirect that loses auth context
 */
@ExceptionHandler(Exception.class)
public ProblemDetail handleGenericException(Exception ex) {
    // Log the full exception with stack trace
    log.error("Unhandled exception in REST controller: {} - {}",
        ex.getClass().getName(), ex.getMessage(), ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred while processing your request. Please try again."
    );
    problemDetail.setTitle("Internal Server Error");

    // Include exception details in development (never in production)
    problemDetail.setProperty("timestamp", LocalDateTime.now());

    // TODO: Add environment check
    // if (isDevelopmentEnvironment()) {
    //     problemDetail.setProperty("exceptionClass", ex.getClass().getName());
    //     problemDetail.setProperty("exceptionMessage", ex.getMessage());
    // }

    return problemDetail;
}
```

**Rationale**:
- Prevent Spring's default error page redirect (which causes 403)
- Ensure ALL exceptions return proper JSON responses
- Maintain security context throughout error handling
- Provide debugging information in development

---

## Investigation Steps for Dev Agent

To identify the underlying backend exception:

1. **Enable trace logging**:
   ```bash
   # In application.yml or via environment
   APP_LOG_LEVEL=TRACE
   SECURITY_LOG_LEVEL=TRACE
   ```

2. **Reproduce the issue** with the specific invoice:
   - Invoice ID: `30db4f6a-0349-4948-bda3-65058c110b8f`
   - Try recording a payment of $50.00

3. **Check logs for**:
   - NullPointerException
   - ClassCastException
   - JSON serialization errors
   - Database constraint violations
   - Transaction rollback messages

4. **Verify database state**:
   ```sql
   -- Check for missing foreign keys
   SELECT * FROM payments WHERE invoice_id = '30db4f6a-0349-4948-bda3-65058c110b8f';

   -- Check invoice state
   SELECT * FROM invoices WHERE id = '30db4f6a-0349-4948-bda3-65058c110b8f';

   -- Check customer exists
   SELECT * FROM customers WHERE id = (
     SELECT customer_id FROM invoices WHERE id = '30db4f6a-0349-4948-bda3-65058c110b8f'
   );
   ```

5. **Test with curl** to bypass frontend:
   ```bash
   curl -X POST http://localhost:8080/api/invoices/30db4f6a-0349-4948-bda3-65058c110b8f/payments \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <JWT_TOKEN>" \
     -d '{
       "paymentDate": "2025-11-08",
       "amount": 50.00,
       "paymentMethod": "CreditCard",
       "reference": "TEST-REF-001",
       "notes": "Test payment"
     }' \
     -v  # Verbose to see full response
   ```

---

## Test Coverage Required

### Unit Tests

**Test File**: `backend/src/test/java/com/invoiceme/payment/commands/RecordPaymentCommandHandlerTest.java`

```java
@Test
@DisplayName("Should throw InvoiceNotSentException for Draft invoice")
void shouldThrowExceptionWhenPaymentAppliedToDraftInvoice() {
    // Given: Draft invoice
    Invoice invoice = InvoiceTestBuilder.aInvoice()
        .withStatus(InvoiceStatus.Draft)
        .build();
    invoice = invoiceRepository.save(invoice);

    RecordPaymentCommand command = new RecordPaymentCommand(
        invoice.getId(),
        LocalDate.now(),
        new BigDecimal("100.00"),
        PaymentMethod.CASH,
        "REF-789",
        null,
        null
    );

    // When/Then: Recording payment should fail with specific exception
    assertThatThrownBy(() -> handler.handle(command, "user@example.com"))
        .isInstanceOf(InvoiceNotSentException.class)
        .hasMessageContaining("Draft")  // Must say "Draft"
        .hasMessageContaining("Sent status");  // Must explain requirement

    // Verify no payment was created
    List<Payment> payments = paymentRepository.findByInvoiceId(invoice.getId());
    assertThat(payments).isEmpty();
}
```

### API Integration Tests

**Test File**: `backend/src/test/java/com/invoiceme/payment/api/PaymentCommandControllerTest.java`

```java
@Test
@WithMockUser
@DisplayName("POST /payments should return 400 (not 403) for Draft invoice")
void shouldReturn400BadRequestForDraftInvoice() throws Exception {
    // Given: Draft invoice in database
    Invoice draft = createAndSaveDraftInvoice();

    String requestBody = """
        {
          "paymentDate": "2025-11-08",
          "amount": 100.00,
          "paymentMethod": "CreditCard",
          "reference": "TEST-001"
        }
        """;

    // When: POST payment to draft invoice
    mockMvc.perform(post("/api/invoices/{id}/payments", draft.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        // Then: Should return 400 Bad Request (NOT 403 Forbidden!)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Invalid Invoice Status"))
        .andExpect(jsonPath("$.detail").value(containsString("Draft")))
        .andExpect(jsonPath("$.detail").value(containsString("Sent status")))
        .andExpect(jsonPath("$.currentStatus").value("Draft"))
        .andExpect(jsonPath("$.invoiceId").exists());
}

@Test
@WithMockUser
@DisplayName("POST /payments should return proper ProblemDetail for all errors")
void shouldReturnProblemDetailForAllErrors() throws Exception {
    // Given: Invoice that will cause various errors
    UUID nonExistentId = UUID.randomUUID();

    // When: POST to non-existent invoice
    mockMvc.perform(post("/api/invoices/{id}/payments", nonExistentId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(validPaymentJson()))
        // Then: Should return ProblemDetail (not redirect to /error)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.detail").exists())
        // Should NOT return HTML error page
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
}
```

### E2E Tests

**Test File**: `e2e/payment-recording.spec.ts`

```typescript
test('should display backend error message when payment fails', async ({ page }) => {
  // Given: Draft invoice
  const invoice = await createDraftInvoice({
    customerName: 'Test Customer',
    totalAmount: 500.00,
  });

  await page.goto(`/invoices/${invoice.id}`);

  // Verify invoice shows as Draft
  await expect(page.getByText('Draft')).toBeVisible();

  // When: Attempt to record payment (button should be disabled)
  const paymentButton = page.getByRole('button', { name: /record payment/i });
  await expect(paymentButton).toBeDisabled();

  // Hover to see tooltip
  await paymentButton.hover();

  // Then: Should show correct tooltip (not hardcoded frontend message)
  await expect(page.getByText(/send invoice first/i)).toBeVisible();
});

test('should successfully record payment on Sent invoice', async ({ page }) => {
  // Given: Sent invoice
  const invoice = await createAndSendInvoice({
    customerName: 'Test Customer',
    totalAmount: 500.00,
  });

  await page.goto(`/invoices/${invoice.id}`);

  // Verify invoice shows as Sent
  await expect(page.getByText('Sent')).toBeVisible();

  // When: Record payment
  await page.click('[data-testid="record-payment-button"]');

  await page.fill('[name="amount"]', '250.00');
  await page.selectOption('[name="paymentMethod"]', 'CreditCard');
  await page.fill('[name="reference"]', 'TEST-PAYMENT-001');

  await page.click('button[type="submit"]');

  // Then: Should show success message (NOT error about draft invoice)
  await expect(page.getByText(/payment.*250.*recorded successfully/i)).toBeVisible();

  // Should NOT show the misleading draft error
  await expect(page.getByText(/draft invoice/i)).not.toBeVisible();

  // Verify balance updated
  await expect(page.getByText(/balance.*250\.00/i)).toBeVisible();
});

test('should display accurate backend error messages', async ({ page }) => {
  // Given: Sent invoice with low balance
  const invoice = await createAndSendInvoice({
    customerName: 'Test Customer',
    totalAmount: 100.00,
    balance: 100.00,
  });

  await page.goto(`/invoices/${invoice.id}`);

  // When: Try to pay more than balance
  await page.click('[data-testid="record-payment-button"]');
  await page.fill('[name="amount"]', '150.00');  // Exceeds balance
  await page.selectOption('[name="paymentMethod"]', 'CreditCard');
  await page.fill('[name="reference"]', 'TEST-OVERPAY');
  await page.click('button[type="submit"]');

  // Then: Should show backend's actual error message
  await expect(page.getByText(/exceeds.*balance/i)).toBeVisible();

  // Should include specific values from backend
  await expect(page.getByText(/150/)).toBeVisible();  // Payment amount
  await expect(page.getByText(/100/)).toBeVisible();  // Invoice balance
});
```

---

## Acceptance Criteria for Fix

- [ ] Frontend error handling uses backend's `detail` field from ProblemDetail
- [ ] HTTP 400 and 403 errors both extract actual error message (no hardcoding)
- [ ] Backend logs ALL exceptions with full stack traces
- [ ] GlobalExceptionHandler catches all exceptions and returns ProblemDetail JSON
- [ ] No redirects to `/error` endpoint during API request processing
- [ ] User sees accurate error messages matching actual invoice state
- [ ] Unit test confirms Draft invoice throws `InvoiceNotSentException` with "Draft" in message
- [ ] API test confirms Draft invoice returns 400 (not 403) with ProblemDetail
- [ ] E2E test confirms users see backend's actual error messages
- [ ] Payment recording succeeds for Sent invoices with valid data
- [ ] Investigate and fix root cause of 403 error (unhandled exception)

---

## Impact Assessment

**User Experience**:
- **Current**: Confusing, misleading error messages prevent users from understanding problems
- **Post-Fix**: Clear, accurate error messages guide users to correct actions

**Development Velocity**:
- **Current**: Debugging issues is difficult due to masked backend errors
- **Post-Fix**: Proper error logging and responses speed up issue resolution

**Data Integrity**:
- **Current**: No risk - transactions roll back correctly
- **Post-Fix**: No change (already safe)

**Security**:
- **Current**: No vulnerability - authentication works correctly
- **Post-Fix**: Improved (proper exception handling prevents auth context loss)

---

## Related Issues

- Story 3.2: Payment Recording Commands (backend implementation)
- Story 3.4: Payment Recording UI (frontend implementation)
- PRD Epic 3 AC 4: "Command validation prevents payment on Draft invoices" ✓ Works, but error message propagation broken

---

## Additional Notes

### Why Frontend Error Mapping is Problematic

HTTP status codes are **semantic** but not **specific**:

- **400 Bad Request**: Can mean validation error, business rule violation, malformed JSON, etc.
- **403 Forbidden**: Can mean missing auth, insufficient permissions, invalid token, business rule violation, etc.
- **404 Not Found**: Can mean invoice not found, customer not found, endpoint not found, etc.

**The backend's `ProblemDetail.detail` field exists specifically to provide the actual error explanation.** Hardcoding frontend error messages based on HTTP codes defeats this purpose.

### Example of Proper Error Response

**Backend should return** (for Draft invoice):
```json
{
  "status": 400,
  "title": "Invalid Invoice Status",
  "detail": "Cannot apply payment to Draft invoice. Invoice must be in Sent status to accept payments. Invoice ID: 30db4f6a-0349-4948-bda3-65058c110b8f",
  "invoiceId": "30db4f6a-0349-4948-bda3-65058c110b8f",
  "currentStatus": "Draft"
}
```

**Frontend should extract and display**:
```
"Cannot apply payment to Draft invoice. Invoice must be in Sent status to accept payments. (Invoice status: Draft)"
```

**NOT**:
```
"Cannot record payment on a draft invoice, please send the invoice first."
```

---

## Attachments

- Database query results: Confirms invoice status is "Sent"
- Server logs: Shows 403 from `/error` redirect
- Code locations: Specific files and line numbers for all issues
- Proposed fixes: Complete code changes ready for implementation

---

## Next Steps

1. **Dev Agent**: Implement Fix #1 (Frontend error handling) - 30 minutes
2. **Dev Agent**: Implement Fix #2 (Backend logging) - 15 minutes
3. **Dev Agent**: Implement Fix #3 (Catch-all exception handler) - 20 minutes
4. **Dev Agent**: Reproduce with trace logging to find root cause - 30 minutes
5. **Dev Agent**: Add test coverage (unit + integration + E2E) - 2 hours
6. **QA Agent**: Verify fixes with original reproduction steps
7. **QA Agent**: Run regression tests on payment flow

**Estimated Fix Time**: 4-5 hours total

---

**Report Generated By**: Quinn (QA Agent)
**Report Date**: 2025-11-08
**Format Version**: 1.0
