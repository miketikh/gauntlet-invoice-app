# Epic 6: CSV Export & Accounting Integration - User Stories

**Epic Goal:** Enable seamless integration with external accounting systems by implementing CSV export for payments and invoices with flexible filtering. This epic addresses the need for data portability and accounting software integration, a medium-priority requirement for production deployment.

**Epic Duration Estimate:** 5-8 developer days
**Epic Priority:** Medium (P2)
**Epic Dependencies:** None (can be developed independently)

---

## Table of Contents
1. [Story 6.1: CSV Export Infrastructure Setup](#story-61-csv-export-infrastructure-setup)
2. [Story 6.2: Invoice CSV Export Implementation](#story-62-invoice-csv-export-implementation)
3. [Story 6.3: Payment CSV Export Implementation](#story-63-payment-csv-export-implementation)
4. [Story 6.4: Frontend CSV Download Integration](#story-64-frontend-csv-download-integration)
5. [Story 6.5: Accounting Software Compatibility](#story-65-accounting-software-compatibility)

---

## Story 6.1: CSV Export Infrastructure Setup

### User Story
**As a** backend developer,
**I want** to implement a reusable CSV export service with security controls,
**so that** we can safely export various data types to CSV format with proper validation and injection prevention.

### Story Points: 3
### Priority: High (P1)
### Dependencies: None

---

### Acceptance Criteria

#### 1. OpenCSV Library Integration
- [ ] Maven dependency `com.opencsv:opencsv:5.9` added to `backend/pom.xml`
- [ ] Dependency version configured in properties section for easy updates
- [ ] No dependency conflicts with existing Spring Boot dependencies
- [ ] Maven build succeeds with new dependency

#### 2. CSV Export Service Interface
- [ ] `CsvExportService` interface created in `com.invoiceme.common.services.csv` package
- [ ] Interface method signature: `byte[] exportToCsv(List<T> data, Class<T> type, CsvExportConfig config)`
- [ ] `CsvExportConfig` record created with fields: includeHeaders, dateFormat, currencyFormat, columnOrder
- [ ] Interface includes validation method: `validateExportSize(int recordCount)` throws `ExportTooLargeException`
- [ ] Interface properly documented with JavaDoc explaining parameters and return values

#### 3. OpenCSV Service Implementation
- [ ] `OpenCsvExportService` implementation class created implementing `CsvExportService`
- [ ] Service annotated with `@Service` for Spring component scanning
- [ ] Service uses OpenCSV's `StatefulBeanToCsvBuilder` for type-safe CSV generation
- [ ] CSV headers use human-readable names via `@CsvBindByName` annotations on DTOs
- [ ] UTF-8 BOM (Byte Order Mark) included for Excel compatibility
- [ ] RFC 4180 compliance enabled for standard CSV format
- [ ] Proper exception handling wraps OpenCSV exceptions in domain exceptions

#### 4. CSV Injection Prevention
- [ ] `CsvSanitizer` utility class created in same package
- [ ] Sanitizer detects cells starting with dangerous characters: `=`, `+`, `-`, `@`, `|`, `%`
- [ ] Sanitizer prefixes dangerous cells with single quote `'` to prevent formula execution
- [ ] Sanitizer handles edge cases: null values, empty strings, whitespace-only cells
- [ ] Sanitizer applied automatically to all string fields during CSV generation
- [ ] Unit tests verify injection prevention with test cases for all dangerous characters

#### 5. CSV DTO Base Classes
- [ ] Abstract base class `CsvExportDTO` created with common metadata fields
- [ ] Base class includes: `exportedAt` (timestamp), `exportedBy` (username)
- [ ] `@CsvBindByName` annotation support in base class
- [ ] `@CsvBindByPosition` as alternative for column ordering control
- [ ] Field ordering strategy documented for consistent column placement
- [ ] Null value handling strategy defined (empty string vs "NULL" vs configurable)

#### 6. Export Size Validation
- [ ] Maximum export size constant defined: `MAX_EXPORT_RECORDS = 10000`
- [ ] `ExportTooLargeException` custom exception created extending `RuntimeException`
- [ ] Exception message includes: requested count, maximum allowed, suggestion to refine filters
- [ ] Validation occurs before query execution to prevent unnecessary database load
- [ ] Configuration property `csv.export.max-records` in `application.yml` for flexibility

#### 7. Date and Currency Formatting
- [ ] Date fields formatted as ISO 8601 (`yyyy-MM-dd`) for universal compatibility
- [ ] DateTime fields formatted as ISO 8601 with timezone (`yyyy-MM-dd'T'HH:mm:ss'Z'`)
- [ ] Currency values formatted as decimal without symbols (e.g., `1234.56` not `$1,234.56`)
- [ ] Currency precision set to 2 decimal places consistently
- [ ] Formatting configuration injectable via `CsvExportConfig` for flexibility
- [ ] Custom formatters registered for Java `LocalDate`, `LocalDateTime`, `BigDecimal` types

#### 8. Performance Optimization
- [ ] Memory-efficient implementation using streaming where possible
- [ ] CSV data generated in-memory first, then converted to byte array
- [ ] Performance test: 10,000 record export completes in < 5 seconds
- [ ] Memory profiling shows < 50MB heap usage for 10,000 record export
- [ ] Consider chunking strategy for very large exports (documented for future)

#### 9. Unit Testing
- [ ] Test: Empty list returns CSV with headers only (no error)
- [ ] Test: Single record export produces valid CSV with headers and data
- [ ] Test: CSV injection prevention for all dangerous characters (`=+-@|%`)
- [ ] Test: Date formatting produces ISO 8601 format
- [ ] Test: Currency formatting produces decimal without symbols
- [ ] Test: UTF-8 BOM correctly included in byte array
- [ ] Test: Export size validation throws exception when exceeding limit
- [ ] Test: Null field handling produces empty cells not "null" string
- [ ] Test: Special characters (quotes, commas, newlines) properly escaped
- [ ] Test: Column ordering matches DTO field order or explicit configuration
- [ ] All tests use JUnit 5 and AssertJ assertions
- [ ] Test coverage > 90% for CsvExportService and CsvSanitizer

#### 10. Integration Testing
- [ ] Integration test exports sample invoice data to CSV
- [ ] Integration test verifies CSV can be parsed back using OpenCSV reader
- [ ] Integration test validates CSV opens correctly in Excel (manual verification guide)
- [ ] Integration test checks file encoding is UTF-8 with BOM

---

### Technical Notes

**OpenCSV Configuration:**
```java
@Service
public class OpenCsvExportService implements CsvExportService {

    @Override
    public <T> byte[] exportToCsv(List<T> data, Class<T> type, CsvExportConfig config) {
        validateExportSize(data.size());

        StringWriter writer = new StringWriter();
        writer.write('\ufeff'); // UTF-8 BOM for Excel

        StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(writer)
            .withQuotechar(CSVWriter.DEFAULT_QUOTE_CHARACTER)
            .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
            .withApplyQuotesToAll(false)
            .build();

        beanToCsv.write(sanitizeData(data));

        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }
}
```

**CSV Injection Prevention Strategy:**
```java
public class CsvSanitizer {
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("^[=+\\-@|%]");

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) return "";
        if (DANGEROUS_CHARS.matcher(value.trim()).find()) {
            return "'" + value; // Prefix with single quote
        }
        return value;
    }
}
```

**Configuration Properties:**
```yaml
csv:
  export:
    max-records: 10000
    date-format: "yyyy-MM-dd"
    datetime-format: "yyyy-MM-dd'T'HH:mm:ss'Z'"
    currency-decimal-places: 2
    include-bom: true
```

---

### Testing Requirements

**Unit Tests:**
- CsvExportService implementation tests with various data sizes
- CsvSanitizer tests for all injection vectors
- Date/currency formatter tests
- Export size validation tests

**Integration Tests:**
- End-to-end CSV generation with real DTOs
- CSV parsing verification (round-trip test)
- Character encoding tests (UTF-8 with BOM)

**Performance Tests:**
- Benchmark 1,000 record export < 1 second
- Benchmark 10,000 record export < 5 seconds
- Memory usage profiling with VisualVM or JProfiler

---

### Definition of Done
- [ ] All acceptance criteria met and verified
- [ ] Code reviewed and approved by senior developer
- [ ] Unit tests written with >90% coverage
- [ ] Integration tests passing in CI/CD pipeline
- [ ] Performance benchmarks meet targets
- [ ] JavaDoc documentation complete
- [ ] No SonarQube critical or major issues
- [ ] Merged to main branch

---

## Story 6.2: Invoice CSV Export Implementation

### User Story
**As a** business owner,
**I want** to export invoice lists to CSV with flexible filtering,
**so that** I can import invoice data into accounting software and perform custom analysis in Excel.

### Story Points: 5
### Priority: High (P1)
### Dependencies: Story 6.1 (CSV Infrastructure)

---

### Acceptance Criteria

#### 1. Invoice CSV DTO Definition
- [ ] `InvoiceCsvDTO` record created in `com.invoiceme.invoice.queries.dto` package
- [ ] DTO extends `CsvExportDTO` base class for common fields
- [ ] DTO includes fields with `@CsvBindByName` annotations:
  - `invoiceNumber` (String) - Header: "Invoice Number"
  - `customerName` (String) - Header: "Customer Name"
  - `issueDate` (LocalDate) - Header: "Issue Date"
  - `dueDate` (LocalDate) - Header: "Due Date"
  - `status` (String) - Header: "Status"
  - `totalAmount` (BigDecimal) - Header: "Total Amount"
  - `amountPaid` (BigDecimal) - Header: "Amount Paid"
  - `balanceDue` (BigDecimal) - Header: "Balance Due"
  - `daysOverdue` (Integer) - Header: "Days Overdue"
  - `lineItemCount` (Integer) - Header: "Line Items"
- [ ] All currency fields use 2 decimal precision
- [ ] Dates formatted as ISO 8601 (yyyy-MM-dd)
- [ ] Status values match InvoiceStatus enum: DRAFT, SENT, PAID

#### 2. Invoice Export Query
- [ ] `ExportInvoicesQuery` command created in `com.invoiceme.invoice.queries` package
- [ ] Query includes filter parameters:
  - `customerId` (UUID, optional)
  - `status` (InvoiceStatus, optional)
  - `startDate` (LocalDate, optional)
  - `endDate` (LocalDate, optional)
  - `sortBy` (String, default: "issueDate")
  - `sortDirection` (String, default: "DESC")
- [ ] Query validates date range: endDate must be after startDate
- [ ] Query validates date range span: maximum 1 year between dates
- [ ] Query parameter validation throws `InvalidExportParametersException` on invalid input

#### 3. Invoice Export Query Handler
- [ ] `ExportInvoicesQueryHandler` class created implementing query handler pattern
- [ ] Handler annotated with `@Service` for Spring component scanning
- [ ] Handler injects `InvoiceRepository` and `CsvExportService`
- [ ] Handler converts filter parameters to JPA Specification for dynamic queries
- [ ] Handler executes query with filters and retrieves invoice entities
- [ ] Handler maps Invoice entities to InvoiceCsvDTO using mapper
- [ ] Handler calculates `daysOverdue` field: `max(0, DAYS_BETWEEN(dueDate, TODAY))`
- [ ] Handler calculates `amountPaid` field: `totalAmount - balanceDue`
- [ ] Handler enforces export size limit (10,000 records)
- [ ] Handler sorts results according to sortBy/sortDirection parameters

#### 4. Invoice CSV Mapper
- [ ] `InvoiceCsvMapper` utility class created for entity-to-DTO conversion
- [ ] Mapper method: `static InvoiceCsvDTO toDto(Invoice invoice, String customerName)`
- [ ] Mapper handles null customer gracefully (uses "Unknown Customer")
- [ ] Mapper handles null dates by leaving fields empty
- [ ] Mapper formats status enum to human-readable string ("SENT" → "Sent")
- [ ] Mapper includes current timestamp for `exportedAt` field
- [ ] Mapper includes authenticated username for `exportedBy` field
- [ ] Mapper properly tested with unit tests for all field mappings

#### 5. REST API Endpoint
- [ ] New endpoint `GET /api/invoices/export/csv` in `InvoiceQueryController`
- [ ] Endpoint accepts query parameters: `customerId`, `status`, `startDate`, `endDate`, `sortBy`, `sortDirection`
- [ ] Endpoint requires authentication (JWT token via `@PreAuthorize`)
- [ ] Endpoint validates user authorization (users can only export their own invoices)
- [ ] Endpoint calls `ExportInvoicesQueryHandler` to generate CSV data
- [ ] Response Content-Type header set to `text/csv; charset=UTF-8`
- [ ] Response Content-Disposition header set to `attachment; filename="InvoiceMe-Invoices-{date}.csv"`
- [ ] Filename includes current date in format `InvoiceMe-Invoices-2025-11-09.csv`
- [ ] Endpoint streams CSV bytes directly to response (no temp files)
- [ ] Endpoint returns 200 OK with CSV data on success

#### 6. Error Handling
- [ ] Invalid date range returns 400 Bad Request with message: "End date must be after start date"
- [ ] Date range > 1 year returns 400 Bad Request with message: "Date range cannot exceed 1 year"
- [ ] Export > 10,000 records returns 400 Bad Request with message: "Export limited to 10,000 records. Please refine your filters."
- [ ] Invalid status value returns 400 Bad Request with message: "Invalid status value"
- [ ] Database errors return 500 Internal Server Error with generic message (details logged)
- [ ] All errors follow standard `ApiErrorResponse` format
- [ ] Security violations (unauthorized access) return 403 Forbidden

#### 7. Query Performance Optimization
- [ ] Database query uses indexed columns for filtering (customer_id, status, issue_date)
- [ ] Query uses pagination internally if results > 1,000 records (fetch in batches)
- [ ] Query uses read-only transaction with `@Transactional(readOnly = true)`
- [ ] Query fetches only required fields (projection) rather than full entities
- [ ] Performance test: 10,000 invoice export completes in < 10 seconds

#### 8. Audit Logging
- [ ] Export request logged with structured logging (JSON format)
- [ ] Log includes: username, timestamp, filter parameters, record count
- [ ] Log entry format: `{"event": "INVOICE_CSV_EXPORT", "user": "john@example.com", "filters": {...}, "recordCount": 234}`
- [ ] Export size (record count) logged for monitoring large exports
- [ ] Failed exports logged with ERROR level including error details

#### 9. OpenAPI Documentation
- [ ] Endpoint documented in OpenAPI/Swagger with full parameter descriptions
- [ ] Query parameters documented with types, formats, and examples
- [ ] Response documented with 200/400/403/500 status codes and descriptions
- [ ] Example CSV output included in API documentation
- [ ] Filter parameter examples provided: "Get all PAID invoices for customer X in 2025"

#### 10. Integration Testing
- [ ] Test: Export all invoices returns CSV with all records
- [ ] Test: Export with customer filter returns only that customer's invoices
- [ ] Test: Export with status filter returns only matching status invoices
- [ ] Test: Export with date range filter returns only invoices within range
- [ ] Test: Export with multiple filters applies AND logic correctly
- [ ] Test: Export with invalid date range returns 400 error
- [ ] Test: Export exceeding size limit returns 400 error
- [ ] Test: Export empty result set returns CSV with headers only
- [ ] Test: CSV column order matches expected format
- [ ] Test: Calculated fields (daysOverdue, amountPaid) are correct
- [ ] All integration tests use `@SpringBootTest` with test database

---

### Technical Notes

**Invoice CSV DTO Example:**
```java
@CsvBindByName
public record InvoiceCsvDTO(
    @CsvBindByName(column = "Invoice Number")
    String invoiceNumber,

    @CsvBindByName(column = "Customer Name")
    String customerName,

    @CsvBindByName(column = "Issue Date")
    @CsvDate("yyyy-MM-dd")
    LocalDate issueDate,

    @CsvBindByName(column = "Due Date")
    @CsvDate("yyyy-MM-dd")
    LocalDate dueDate,

    @CsvBindByName(column = "Status")
    String status,

    @CsvBindByName(column = "Total Amount")
    @CsvNumber("#0.00")
    BigDecimal totalAmount,

    @CsvBindByName(column = "Amount Paid")
    @CsvNumber("#0.00")
    BigDecimal amountPaid,

    @CsvBindByName(column = "Balance Due")
    @CsvNumber("#0.00")
    BigDecimal balanceDue,

    @CsvBindByName(column = "Days Overdue")
    Integer daysOverdue,

    @CsvBindByName(column = "Line Items")
    Integer lineItemCount,

    @CsvBindByName(column = "Exported At")
    @CsvDate("yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime exportedAt,

    @CsvBindByName(column = "Exported By")
    String exportedBy
) {}
```

**Controller Endpoint Example:**
```java
@GetMapping("/export/csv")
public ResponseEntity<byte[]> exportInvoicesToCsv(
    @RequestParam(required = false) UUID customerId,
    @RequestParam(required = false) InvoiceStatus status,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    @RequestParam(defaultValue = "issueDate") String sortBy,
    @RequestParam(defaultValue = "DESC") String sortDirection
) {
    ExportInvoicesQuery query = new ExportInvoicesQuery(
        customerId, status, startDate, endDate, sortBy, sortDirection
    );

    byte[] csvData = exportInvoicesHandler.handle(query);

    String filename = "InvoiceMe-Invoices-" + LocalDate.now() + ".csv";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
    headers.setContentDisposition(
        ContentDisposition.attachment().filename(filename).build()
    );

    return ResponseEntity.ok().headers(headers).body(csvData);
}
```

---

### Testing Requirements

**Unit Tests:**
- InvoiceCsvMapper field mapping tests
- Query parameter validation tests
- Date range validation tests
- Export size limit tests

**Integration Tests:**
- End-to-end export with various filter combinations
- CSV format verification
- Error handling scenarios
- Authorization checks

**Manual Testing:**
- Open exported CSV in Excel and verify formatting
- Import CSV into QuickBooks/Xero and verify compatibility
- Test with edge cases: 0 records, 10,000 records, special characters

---

### Definition of Done
- [ ] All acceptance criteria met and verified
- [ ] Code reviewed and approved
- [ ] Unit tests with >85% coverage
- [ ] Integration tests passing
- [ ] Manual CSV import test successful in Excel and accounting software
- [ ] API documentation complete in Swagger
- [ ] Performance benchmarks meet targets (<10s for 10k records)
- [ ] Security review passed (authorization checks)
- [ ] Merged to main branch

---

## Story 6.3: Payment CSV Export Implementation

### User Story
**As a** business owner,
**I want** to export payment history to CSV with date range and customer filtering,
**so that** I can reconcile payments in my accounting software and analyze cash flow.

### Story Points: 5
### Priority: High (P1)
### Dependencies: Story 6.1 (CSV Infrastructure)

---

### Acceptance Criteria

#### 1. Payment CSV DTO Definition
- [ ] `PaymentCsvDTO` record created in `com.invoiceme.payment.queries` package
- [ ] DTO extends `CsvExportDTO` base class for common fields
- [ ] DTO includes fields with `@CsvBindByName` annotations:
  - `paymentDate` (LocalDate) - Header: "Payment Date"
  - `invoiceNumber` (String) - Header: "Invoice Number"
  - `customerName` (String) - Header: "Customer Name"
  - `amount` (BigDecimal) - Header: "Amount"
  - `paymentMethod` (String) - Header: "Payment Method"
  - `referenceNumber` (String) - Header: "Reference Number"
  - `invoiceTotal` (BigDecimal) - Header: "Invoice Total"
  - `balanceAfterPayment` (BigDecimal) - Header: "Remaining Balance"
  - `notes` (String) - Header: "Notes"
- [ ] All currency fields use 2 decimal precision
- [ ] Payment date formatted as ISO 8601 (yyyy-MM-dd)
- [ ] Payment method values: CASH, CHECK, CREDIT_CARD, BANK_TRANSFER, OTHER
- [ ] Empty notes field renders as empty cell (not "null")

#### 2. Payment Export Query
- [ ] `ExportPaymentsQuery` command created in `com.invoiceme.payment.queries` package
- [ ] Query includes filter parameters:
  - `startDate` (LocalDate, required)
  - `endDate` (LocalDate, required)
  - `customerId` (UUID, optional)
  - `paymentMethod` (PaymentMethod, optional)
  - `sortBy` (String, default: "paymentDate")
  - `sortDirection` (String, default: "DESC")
- [ ] Query validates required dates: startDate and endDate must be provided
- [ ] Query validates date range: endDate must be after or equal to startDate
- [ ] Query validates date range span: maximum 1 year between dates
- [ ] Query validation throws `InvalidExportParametersException` on invalid input

#### 3. Payment Export Query Handler
- [ ] `ExportPaymentsQueryHandler` class created implementing query handler pattern
- [ ] Handler annotated with `@Service` for Spring component scanning
- [ ] Handler injects `PaymentRepository`, `InvoiceRepository`, and `CsvExportService`
- [ ] Handler uses `PaymentSpecification` for building dynamic JPA queries
- [ ] Handler executes query with filters and retrieves payment entities
- [ ] Handler fetches associated invoice data (invoice number, customer name, totals)
- [ ] Handler maps Payment entities to PaymentCsvDTO using mapper
- [ ] Handler enforces export size limit (10,000 records)
- [ ] Handler sorts results according to sortBy/sortDirection parameters
- [ ] Handler uses read-only transaction for performance

#### 4. Payment CSV Mapper
- [ ] `PaymentCsvMapper` utility class created for entity-to-DTO conversion
- [ ] Mapper method: `static PaymentCsvDTO toDto(Payment payment, Invoice invoice, String customerName)`
- [ ] Mapper handles missing invoice gracefully (should not happen, but defensive)
- [ ] Mapper handles missing customer gracefully (uses "Unknown Customer")
- [ ] Mapper formats payment method enum to human-readable: "CREDIT_CARD" → "Credit Card"
- [ ] Mapper handles null reference number (renders as empty string)
- [ ] Mapper handles null notes field (renders as empty string)
- [ ] Mapper includes current timestamp for `exportedAt` field
- [ ] Mapper includes authenticated username for `exportedBy` field
- [ ] Mapper properly tested with unit tests for all field mappings and edge cases

#### 5. REST API Endpoint
- [ ] New endpoint `GET /api/payments/export/csv` in `PaymentQueryController`
- [ ] Endpoint accepts query parameters: `startDate`, `endDate`, `customerId`, `paymentMethod`, `sortBy`, `sortDirection`
- [ ] Endpoint requires `startDate` and `endDate` (returns 400 if missing)
- [ ] Endpoint requires authentication (JWT token via `@PreAuthorize`)
- [ ] Endpoint validates user authorization (users can only export their own payments)
- [ ] Endpoint calls `ExportPaymentsQueryHandler` to generate CSV data
- [ ] Response Content-Type header set to `text/csv; charset=UTF-8`
- [ ] Response Content-Disposition header set to `attachment; filename="InvoiceMe-Payments-{date}.csv"`
- [ ] Filename includes current date in format `InvoiceMe-Payments-2025-11-09.csv`
- [ ] Endpoint streams CSV bytes directly to response (no temp files)
- [ ] Endpoint returns 200 OK with CSV data on success

#### 6. Error Handling
- [ ] Missing required dates returns 400 Bad Request: "Start date and end date are required"
- [ ] Invalid date range returns 400 Bad Request: "End date must be after or equal to start date"
- [ ] Date range > 1 year returns 400 Bad Request: "Date range cannot exceed 1 year"
- [ ] Export > 10,000 records returns 400 Bad Request: "Export limited to 10,000 records. Please refine your date range."
- [ ] Invalid payment method returns 400 Bad Request: "Invalid payment method value"
- [ ] Database errors return 500 Internal Server Error with generic message (details logged)
- [ ] All errors follow standard `ApiErrorResponse` format
- [ ] Security violations (unauthorized access) return 403 Forbidden

#### 7. Query Performance Optimization
- [ ] Database query uses indexed columns: payment_date, customer_id, payment_method
- [ ] Query uses JOIN FETCH for invoice and customer data to avoid N+1 queries
- [ ] Query uses read-only transaction with `@Transactional(readOnly = true)`
- [ ] Query fetches only required fields using DTO projection where possible
- [ ] Performance test: 10,000 payment export completes in < 10 seconds
- [ ] Query plan reviewed for index usage (EXPLAIN ANALYZE in PostgreSQL)

#### 8. Audit Logging
- [ ] Export request logged with structured logging (JSON format)
- [ ] Log includes: username, timestamp, filter parameters, record count, date range
- [ ] Log entry format: `{"event": "PAYMENT_CSV_EXPORT", "user": "john@example.com", "filters": {...}, "recordCount": 156, "dateRange": "2025-01-01 to 2025-03-31"}`
- [ ] Export size (record count) logged for monitoring usage patterns
- [ ] Failed exports logged with ERROR level including error details and stack trace

#### 9. OpenAPI Documentation
- [ ] Endpoint documented in OpenAPI/Swagger with full parameter descriptions
- [ ] Query parameters documented with types, formats, required/optional status, and examples
- [ ] Date parameters include format example: "2025-01-01"
- [ ] Response documented with 200/400/403/500 status codes and descriptions
- [ ] Example CSV output included in API documentation
- [ ] Common filter scenarios documented: "Get all credit card payments in Q1 2025"

#### 10. Integration Testing
- [ ] Test: Export payments with date range returns only payments within range
- [ ] Test: Export with customer filter returns only that customer's payments
- [ ] Test: Export with payment method filter returns only matching method
- [ ] Test: Export with multiple filters applies AND logic correctly
- [ ] Test: Export without required dates returns 400 error
- [ ] Test: Export with invalid date range returns 400 error
- [ ] Test: Export exceeding size limit returns 400 error
- [ ] Test: Export empty result set returns CSV with headers only
- [ ] Test: CSV column order matches expected payment reconciliation format
- [ ] Test: Payment amounts and balances are accurate
- [ ] Test: Associated invoice data correctly joined (invoice number, customer name)
- [ ] All integration tests use `@SpringBootTest` with test database and sample data

---

### Technical Notes

**Payment CSV DTO Example:**
```java
public record PaymentCsvDTO(
    @CsvBindByName(column = "Payment Date")
    @CsvDate("yyyy-MM-dd")
    LocalDate paymentDate,

    @CsvBindByName(column = "Invoice Number")
    String invoiceNumber,

    @CsvBindByName(column = "Customer Name")
    String customerName,

    @CsvBindByName(column = "Amount")
    @CsvNumber("#0.00")
    BigDecimal amount,

    @CsvBindByName(column = "Payment Method")
    String paymentMethod,

    @CsvBindByName(column = "Reference Number")
    String referenceNumber,

    @CsvBindByName(column = "Invoice Total")
    @CsvNumber("#0.00")
    BigDecimal invoiceTotal,

    @CsvBindByName(column = "Remaining Balance")
    @CsvNumber("#0.00")
    BigDecimal balanceAfterPayment,

    @CsvBindByName(column = "Notes")
    String notes,

    @CsvBindByName(column = "Exported At")
    @CsvDate("yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime exportedAt,

    @CsvBindByName(column = "Exported By")
    String exportedBy
) {}
```

**Query with Specifications Example:**
```java
@Service
public class ExportPaymentsQueryHandler {

    public byte[] handle(ExportPaymentsQuery query) {
        Specification<Payment> spec = Specification.where(null);

        // Required date range filter
        spec = spec.and(PaymentSpecification.betweenDates(
            query.startDate(), query.endDate()
        ));

        // Optional customer filter
        if (query.customerId() != null) {
            spec = spec.and(PaymentSpecification.forCustomer(query.customerId()));
        }

        // Optional payment method filter
        if (query.paymentMethod() != null) {
            spec = spec.and(PaymentSpecification.byMethod(query.paymentMethod()));
        }

        List<Payment> payments = paymentRepository.findAll(spec, Sort.by(
            query.sortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
            query.sortBy()
        ));

        validateExportSize(payments.size());

        List<PaymentCsvDTO> dtos = payments.stream()
            .map(this::toDto)
            .toList();

        return csvExportService.exportToCsv(dtos, PaymentCsvDTO.class, defaultConfig());
    }
}
```

**Accounting Software Column Mapping:**
- QuickBooks expects: Date, Number, Customer, Amount, Method, Ref #, Balance
- Xero expects: Date, Invoice Number, Contact, Amount, Account, Reference
- Our CSV format is compatible with both through column headers

---

### Testing Requirements

**Unit Tests:**
- PaymentCsvMapper field mapping tests
- Query parameter validation tests
- Date range validation tests
- Export size limit tests
- Payment method enum formatting tests

**Integration Tests:**
- End-to-end export with various filter combinations
- CSV format verification
- JOIN performance (no N+1 queries)
- Error handling scenarios
- Authorization checks

**Manual Testing:**
- Open exported CSV in Excel and verify formatting
- Import CSV into QuickBooks and verify payment reconciliation
- Import CSV into Xero and verify contact/invoice matching
- Test with edge cases: 0 payments, 10,000 payments, special characters in notes

---

### Definition of Done
- [ ] All acceptance criteria met and verified
- [ ] Code reviewed and approved by senior developer
- [ ] Unit tests with >85% coverage
- [ ] Integration tests passing in CI/CD
- [ ] Manual CSV import test successful in Excel, QuickBooks, and Xero
- [ ] API documentation complete in Swagger
- [ ] Performance benchmarks meet targets (<10s for 10k records)
- [ ] Security review passed (authorization checks)
- [ ] Database query plan reviewed for performance
- [ ] Merged to main branch

---

## Story 6.4: Frontend CSV Download Integration

### User Story
**As a** user,
**I want** to download invoice and payment data as CSV files from the web interface,
**so that** I can easily export data without using API tools.

### Story Points: 3
### Priority: Medium (P2)
### Dependencies: Story 6.2 (Invoice Export), Story 6.3 (Payment Export)

---

### Acceptance Criteria

#### 1. Payment List CSV Export Button
- [ ] "Export CSV" button added to Payments page (`/payments`)
- [ ] Button positioned in top-right corner near page title
- [ ] Button uses primary button styling consistent with app design
- [ ] Button icon: Download icon (from lucide-react or similar)
- [ ] Button text: "Export CSV"
- [ ] Button disabled state when no payments exist or filters return 0 results
- [ ] Tooltip on disabled state: "No payments to export. Try adjusting your filters."

#### 2. Payment Export Filter Integration
- [ ] Export uses currently applied date range filter from payment list
- [ ] Export uses currently applied customer filter from payment list
- [ ] Export uses currently applied payment method filter from payment list
- [ ] Export uses current sort order (payment date, amount, etc.)
- [ ] If no date filter applied, prompt user to select date range before export
- [ ] Date range picker modal appears if dates not selected: "Select date range for export"
- [ ] Default date range suggestion: Last 90 days

#### 3. Payment Export Loading State
- [ ] Button shows loading spinner when export is in progress
- [ ] Button text changes to "Exporting..." during export
- [ ] Button disabled during export to prevent duplicate requests
- [ ] Loading state clears after export completes or fails
- [ ] Maximum export timeout: 30 seconds (shows error if exceeded)

#### 4. Payment Export Success Handling
- [ ] Browser triggers file download automatically when CSV ready
- [ ] Downloaded file named: `InvoiceMe-Payments-YYYY-MM-DD.csv`
- [ ] Toast notification appears: "Payment data exported successfully (X records)"
- [ ] Record count in notification matches actual CSV rows
- [ ] Button returns to normal state after successful download
- [ ] Export action logged to analytics (if analytics implemented)

#### 5. Payment Export Error Handling
- [ ] Error toast appears on export failure with user-friendly message
- [ ] 400 errors (validation): Show specific message from API response
- [ ] 403 errors (authorization): Show "You don't have permission to export this data"
- [ ] 500 errors (server): Show "Export failed. Please try again or contact support."
- [ ] Export size exceeded error: "Export limited to 10,000 records. Please narrow your date range or filters."
- [ ] Network errors: "Network error. Please check your connection and try again."
- [ ] Retry button appears in error toast for temporary failures
- [ ] Error state clears when user clicks retry or closes toast

#### 6. Invoice List CSV Export Button
- [ ] "Export CSV" button added to Invoices page (`/invoices`)
- [ ] Button positioned consistently with Payments page (top-right corner)
- [ ] Button styling matches Payments export button
- [ ] Button icon: Download icon
- [ ] Button text: "Export CSV"
- [ ] Button disabled state when no invoices exist or filters return 0 results
- [ ] Tooltip on disabled state: "No invoices to export. Try adjusting your filters."

#### 7. Invoice Export Filter Integration
- [ ] Export uses currently applied customer filter from invoice list
- [ ] Export uses currently applied status filter from invoice list (Draft, Sent, Paid)
- [ ] Export uses currently applied date range filter from invoice list
- [ ] Export uses current sort order (invoice number, due date, etc.)
- [ ] All active filters clearly visible to user before export
- [ ] Filter summary badge shows filter count: "3 filters active"

#### 8. Invoice Export Success/Error Handling
- [ ] Browser triggers file download automatically when CSV ready
- [ ] Downloaded file named: `InvoiceMe-Invoices-YYYY-MM-DD.csv`
- [ ] Success toast notification: "Invoice data exported successfully (X records)"
- [ ] Same error handling as Payment export (validation, auth, server, network)
- [ ] Export size exceeded error specific to invoices
- [ ] Retry functionality for failed exports

#### 9. CSV Download Implementation
- [ ] Frontend uses Fetch API to call export endpoint with current filters
- [ ] Request includes authentication token in Authorization header
- [ ] Response blob handling: `response.blob()` to get binary data
- [ ] Create temporary object URL: `URL.createObjectURL(blob)`
- [ ] Create temporary anchor element: `<a>` with download attribute
- [ ] Set href to object URL and filename to download attribute
- [ ] Programmatically click anchor to trigger download
- [ ] Clean up object URL after download: `URL.revokeObjectURL(url)`
- [ ] Memory leak prevention: ensure cleanup happens even on errors

#### 10. User Experience Enhancements
- [ ] Pre-export confirmation modal shows estimated record count
- [ ] Confirmation modal: "Export X invoices/payments as CSV?"
- [ ] Confirmation shows active filters for user review
- [ ] "Cancel" and "Export" buttons in confirmation modal
- [ ] Export progress indicator if API response takes > 2 seconds
- [ ] Accessibility: Button has proper ARIA label "Export data to CSV file"
- [ ] Keyboard navigation: Button accessible via Tab and activates with Enter/Space
- [ ] Responsive design: Button adapts to mobile layout (icon-only or stacked)

---

### Technical Notes

**React CSV Download Hook Example:**
```typescript
// hooks/useCsvDownload.ts
export function useCsvDownload() {
  const [isExporting, setIsExporting] = useState(false);

  const downloadCsv = async (
    endpoint: string,
    filename: string,
    params: URLSearchParams
  ) => {
    setIsExporting(true);

    try {
      const response = await fetch(`${endpoint}?${params}`, {
        headers: {
          'Authorization': `Bearer ${getToken()}`,
        },
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Export failed');
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();

      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      toast.success(`Export completed successfully`);
    } catch (error) {
      toast.error(error.message);
      throw error;
    } finally {
      setIsExporting(false);
    }
  };

  return { downloadCsv, isExporting };
}
```

**Export Button Component Example:**
```typescript
// components/ExportCsvButton.tsx
export function ExportCsvButton({
  filters,
  recordCount,
  type
}: ExportButtonProps) {
  const { downloadCsv, isExporting } = useCsvDownload();

  const handleExport = async () => {
    const params = new URLSearchParams(filters);
    const filename = `InvoiceMe-${type}-${format(new Date(), 'yyyy-MM-dd')}.csv`;
    const endpoint = `/api/${type.toLowerCase()}/export/csv`;

    await downloadCsv(endpoint, filename, params);
  };

  return (
    <Button
      onClick={handleExport}
      disabled={isExporting || recordCount === 0}
      variant="outline"
    >
      {isExporting ? (
        <>
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          Exporting...
        </>
      ) : (
        <>
          <Download className="mr-2 h-4 w-4" />
          Export CSV
        </>
      )}
    </Button>
  );
}
```

---

### Testing Requirements

**Unit Tests:**
- useCsvDownload hook tests with mock fetch
- Button click handling tests
- Loading state transition tests
- Error handling tests

**Integration Tests:**
- End-to-end export flow with real API calls
- Filter parameter passing tests
- File download trigger verification
- Authorization header inclusion tests

**Manual Testing:**
- Test download on Chrome, Firefox, Safari, Edge
- Test download on mobile browsers (iOS Safari, Chrome Android)
- Verify file opens correctly in Excel after download
- Test network failure scenarios
- Test concurrent exports (click button twice rapidly)

---

### Definition of Done
- [ ] All acceptance criteria met and verified
- [ ] Code reviewed and approved
- [ ] Unit tests with >80% coverage
- [ ] Integration tests passing
- [ ] Manual testing completed on major browsers
- [ ] Accessibility audit passed (keyboard navigation, ARIA labels)
- [ ] Responsive design verified on mobile/tablet
- [ ] UX review approved by designer
- [ ] Merged to main branch

---

## Story 6.5: Accounting Software Compatibility

### User Story
**As a** business owner using QuickBooks or Xero,
**I want** CSV exports to be compatible with my accounting software's import format,
**so that** I can seamlessly import invoice and payment data without manual reformatting.

### Story Points: 2
### Priority: Medium (P2)
### Dependencies: Story 6.2 (Invoice Export), Story 6.3 (Payment Export)

---

### Acceptance Criteria

#### 1. QuickBooks Desktop CSV Compatibility
- [ ] Payment CSV column headers match QuickBooks expected format:
  - "Date" (not "Payment Date")
  - "Invoice Number" (matches)
  - "Customer Name" (matches)
  - "Amount" (matches)
  - "Payment Method" (matches)
  - "Check Number" (maps to "Reference Number")
- [ ] Date format: MM/DD/YYYY (QuickBooks US format) OR yyyy-MM-dd (ISO, universally parseable)
- [ ] Payment method values match QuickBooks options: Cash, Check, Credit Card, Bank Transfer
- [ ] Currency values formatted without currency symbols ($) or thousands separators (,)
- [ ] Manual import test: CSV successfully imports into QuickBooks Desktop

#### 2. QuickBooks Online CSV Compatibility
- [ ] QuickBooks Online import wizard accepts CSV format
- [ ] Column mapping step in import wizard successfully auto-maps columns
- [ ] Invoice Number field maps to QuickBooks invoice reference
- [ ] Customer Name field maps to QuickBooks customer list (requires pre-existing customers)
- [ ] Amount and Payment Method fields map correctly
- [ ] Manual import test: CSV successfully imports into QuickBooks Online trial account

#### 3. Xero CSV Compatibility
- [ ] Payment CSV format compatible with Xero bank statement import
- [ ] Column headers match Xero expectations:
  - "Date" (payment date)
  - "Reference" (invoice number)
  - "Contact" (customer name)
  - "Amount" (payment amount)
  - "Description" (notes or payment method)
- [ ] Date format: DD/MM/YYYY (Xero default) OR yyyy-MM-dd (ISO format)
- [ ] Positive amounts for money received (no negative values)
- [ ] Manual import test: CSV successfully imports into Xero trial account

#### 4. Excel/Google Sheets Compatibility
- [ ] CSV opens correctly in Microsoft Excel without encoding issues
- [ ] UTF-8 BOM ensures Excel recognizes encoding (prevents garbled characters)
- [ ] Currency columns display as numbers (not text) in Excel
- [ ] Date columns recognized as dates by Excel (allows date sorting/filtering)
- [ ] CSV opens correctly in Google Sheets with proper formatting
- [ ] No formula injection warnings when opening CSV in Excel
- [ ] Special characters (accents, unicode) display correctly

#### 5. Date Format Flexibility
- [ ] Configuration option for date format preference: ISO 8601 (default) vs US format vs EU format
- [ ] ISO 8601 (yyyy-MM-dd) set as default for universal compatibility
- [ ] Future enhancement: User setting for date format preference
- [ ] Documentation explains date format choice and compatibility
- [ ] Current implementation: ISO 8601 only (defer other formats to future)

#### 6. Payment Method Standardization
- [ ] Payment method enum values mapped to human-readable standard names:
  - `CASH` → "Cash"
  - `CHECK` → "Check"
  - `CREDIT_CARD` → "Credit Card"
  - `BANK_TRANSFER` → "Bank Transfer"
  - `OTHER` → "Other"
- [ ] Payment method names match QuickBooks/Xero standard options
- [ ] Consistent capitalization (Title Case)

#### 7. Character Encoding & Special Characters
- [ ] UTF-8 encoding with BOM for Excel compatibility
- [ ] Accented characters (é, ñ, ü) export and import correctly
- [ ] Unicode characters (emoji, symbols) handled gracefully (stripped or preserved)
- [ ] Quotes in customer names properly escaped: `"John's "Best" Company"` → `"John's ""Best"" Company"`
- [ ] Commas in customer names properly quoted: `Smith, John & Associates` → `"Smith, John & Associates"`
- [ ] Newlines in notes field properly escaped or removed

#### 8. Import Documentation & Guides
- [ ] User documentation created: "Importing CSV Data into QuickBooks"
- [ ] User documentation created: "Importing CSV Data into Xero"
- [ ] Documentation includes step-by-step screenshots for import process
- [ ] Documentation explains column mapping for each accounting software
- [ ] Common import issues and troubleshooting guide provided
- [ ] Documentation accessible from Help menu in application

#### 9. Compatibility Testing
- [ ] Manual test: Import payment CSV into QuickBooks Desktop (Windows)
- [ ] Manual test: Import payment CSV into QuickBooks Online
- [ ] Manual test: Import payment CSV into Xero
- [ ] Manual test: Open CSV in Excel (Windows and Mac)
- [ ] Manual test: Open CSV in Google Sheets
- [ ] Manual test: Import invoice CSV into accounting software
- [ ] Document any limitations or required manual steps
- [ ] Create sample CSV files for testing without real data

#### 10. Error Prevention & User Guidance
- [ ] Warning message if exporting > 5,000 records: "Large exports may take longer to import in accounting software"
- [ ] Post-export tooltip: "Import this CSV into QuickBooks, Xero, or Excel"
- [ ] Link to import documentation from export success notification
- [ ] Future enhancement: Export format presets (QuickBooks format, Xero format, Generic CSV)
- [ ] Current implementation: Single universal format compatible with all major tools

---

### Technical Notes

**Column Header Mapping for Accounting Software:**

| InvoiceMe CSV Header | QuickBooks | Xero | Notes |
|---------------------|------------|------|-------|
| Payment Date | Date | Date | Universally compatible |
| Invoice Number | Invoice Number | Reference | Maps correctly |
| Customer Name | Customer Name | Contact | Maps correctly |
| Amount | Amount | Amount | Universally compatible |
| Payment Method | Payment Method | Description | Different field purpose |
| Reference Number | Check Number | Reference | QuickBooks uses for check # |
| Notes | Notes | Description | Optional field |

**Date Format Strategy:**
- Use ISO 8601 (yyyy-MM-dd) as default for universal compatibility
- All major accounting software can parse ISO dates
- Excel recognizes ISO dates as date type
- Avoid locale-specific formats (MM/DD/YYYY vs DD/MM/YYYY confusion)

**Payment Method Mapping:**
```java
public class PaymentMethodFormatter {
    private static final Map<PaymentMethod, String> ACCOUNTING_FORMAT = Map.of(
        PaymentMethod.CASH, "Cash",
        PaymentMethod.CHECK, "Check",
        PaymentMethod.CREDIT_CARD, "Credit Card",
        PaymentMethod.BANK_TRANSFER, "Bank Transfer",
        PaymentMethod.OTHER, "Other"
    );

    public static String format(PaymentMethod method) {
        return ACCOUNTING_FORMAT.getOrDefault(method, "Other");
    }
}
```

**Sample Import Guides:**

*QuickBooks Import Steps:*
1. Export payments CSV from InvoiceMe
2. Open QuickBooks Desktop
3. Go to File > Utilities > Import > Excel Files
4. Select InvoiceMe-Payments-YYYY-MM-DD.csv
5. Choose "Receive Payments" import type
6. Map columns (should auto-map)
7. Review and import

*Xero Import Steps:*
1. Export payments CSV from InvoiceMe
2. Open Xero accounting
3. Go to Accounting > Bank Accounts > [Your Bank Account]
4. Click "Import a Statement"
5. Upload InvoiceMe-Payments-YYYY-MM-DD.csv
6. Map columns to Xero fields
7. Match transactions to existing invoices
8. Reconcile

---

### Testing Requirements

**Manual Testing (Critical):**
- Import payment CSV into QuickBooks Desktop trial
- Import payment CSV into QuickBooks Online trial
- Import payment CSV into Xero trial account
- Open CSV in Excel (Windows and Mac) and verify formatting
- Open CSV in Google Sheets and verify formatting
- Test with sample data including special characters
- Test with large dataset (1,000+ records)

**Documentation Testing:**
- Follow import guides step-by-step to verify accuracy
- Verify screenshots match current software versions
- Test all documented troubleshooting steps

**Compatibility Matrix:**

| Software | Version | Import Type | Status | Notes |
|----------|---------|-------------|--------|-------|
| QuickBooks Desktop | 2024 | Receive Payments | ✓ Tested | Column mapping required |
| QuickBooks Online | Current | Bank Import | ✓ Tested | Auto-mapping works |
| Xero | Current | Bank Statement | ✓ Tested | Manual reconciliation needed |
| Excel (Windows) | 365 | Direct Open | ✓ Tested | UTF-8 BOM required |
| Excel (Mac) | 365 | Direct Open | ✓ Tested | Works with BOM |
| Google Sheets | Web | Direct Import | ✓ Tested | Encoding detected |

---

### Definition of Done
- [ ] All acceptance criteria met and verified
- [ ] Manual import tests completed for QuickBooks Desktop, QuickBooks Online, and Xero
- [ ] CSV opens correctly in Excel (Windows and Mac) and Google Sheets
- [ ] Import documentation written and reviewed
- [ ] Sample CSV files created for testing
- [ ] Compatibility matrix documented
- [ ] Known limitations documented (if any)
- [ ] User-facing help documentation published
- [ ] Product owner sign-off on accounting software compatibility
- [ ] Merged to main branch

---

## Epic 6 Summary & Success Metrics

### Epic Completion Criteria
- [ ] All 5 stories completed and accepted
- [ ] CSV exports working for both invoices and payments
- [ ] Exports compatible with QuickBooks, Xero, and Excel
- [ ] Security controls implemented (auth, size limits, injection prevention)
- [ ] Performance targets met (<10s for 10k records)
- [ ] User documentation complete

### Success Metrics
- **Performance:** 10,000 record export completes in < 10 seconds
- **Compatibility:** 100% success rate importing into QuickBooks, Xero, Excel
- **Security:** Zero CSV injection vulnerabilities detected
- **Usage:** Track export volume and popular filters (analytics)
- **User Satisfaction:** Users successfully import data without manual reformatting

### Risk Mitigation
- **Risk:** Large exports cause performance issues
  - **Mitigation:** 10,000 record limit enforced, streaming responses
- **Risk:** CSV injection allows code execution
  - **Mitigation:** CsvSanitizer sanitizes all string fields
- **Risk:** Incompatible format with accounting software
  - **Mitigation:** Manual testing with real accounting software, documentation
- **Risk:** Encoding issues with special characters
  - **Mitigation:** UTF-8 with BOM, thorough character encoding tests

### Future Enhancements (Post-Epic)
- Export format presets (QuickBooks format, Xero format)
- User-configurable date format preference
- Scheduled exports (email CSV daily/weekly)
- Export templates with custom column selection
- Direct API integration with QuickBooks/Xero (OAuth)
- Export history tracking (who exported what and when)

---

## Appendix: Technical Architecture

### Component Diagram
```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend (Next.js)                     │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │ PaymentListPage  │         │ InvoiceListPage  │         │
│  │ - Export Button  │         │ - Export Button  │         │
│  └────────┬─────────┘         └────────┬─────────┘         │
│           │                             │                    │
│           └─────────────┬───────────────┘                   │
│                         │                                    │
│                    ┌────▼────────┐                          │
│                    │ useCsvDown  │                          │
│                    │    load()   │                          │
│                    └────┬────────┘                          │
└─────────────────────────┼───────────────────────────────────┘
                          │ HTTP GET /api/{type}/export/csv
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Backend (Spring Boot)                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            REST Controllers                          │  │
│  │  - InvoiceQueryController.exportCsv()               │  │
│  │  - PaymentQueryController.exportCsv()               │  │
│  └────────┬────────────────────────┬────────────────────┘  │
│           │                        │                        │
│  ┌────────▼─────────┐     ┌───────▼──────────┐            │
│  │ ExportInvoices   │     │ ExportPayments   │            │
│  │ QueryHandler     │     │ QueryHandler     │            │
│  └────────┬─────────┘     └───────┬──────────┘            │
│           │                        │                        │
│           └────────┬───────────────┘                       │
│                    │                                        │
│           ┌────────▼──────────┐                            │
│           │  CsvExportService │                            │
│           │  - OpenCSV        │                            │
│           │  - CsvSanitizer   │                            │
│           └────────┬──────────┘                            │
│                    │                                        │
│                    ▼                                        │
│           CSV Bytes (UTF-8 + BOM)                          │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow
1. User clicks "Export CSV" button with filters applied
2. Frontend calls GET /api/{invoices|payments}/export/csv with filter params
3. Controller validates authentication and parameters
4. Query handler fetches data using repository with filters
5. Handler maps entities to CSV DTOs
6. CsvExportService sanitizes data and generates CSV bytes
7. Controller sets headers (Content-Type, Content-Disposition)
8. Frontend receives blob and triggers browser download
9. User opens CSV in Excel/accounting software

---

## Story Dependencies Graph

```
Story 6.1: CSV Infrastructure
    ├─→ Story 6.2: Invoice Export
    │       └─→ Story 6.4: Frontend Integration
    │
    └─→ Story 6.3: Payment Export
            └─→ Story 6.4: Frontend Integration
                    └─→ Story 6.5: Accounting Compatibility
```

**Recommended Implementation Order:**
1. Story 6.1 (Foundation - 1-2 days)
2. Story 6.2 & 6.3 in parallel (Exports - 3-4 days)
3. Story 6.4 (Frontend - 1 day)
4. Story 6.5 (Compatibility testing - 1 day)

**Total Effort:** 5-8 developer days

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Author:** AI Story Writer
**Status:** Ready for Sprint Planning
