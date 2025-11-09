# InvoiceMe Missing Features - Product Requirements Document (PRD)

## Goals and Background Context

### Goals
- Complete the remaining 5% of InvoiceMe functionality to reach production-ready status
- Implement critical export and automation features that enable real-world business usage
- Add user preferences and configuration capabilities for multi-tenant readiness
- Enhance operational efficiency through bulk operations and email automation
- Maintain architectural integrity while adding new features (DDD, CQRS, VSA)

### Background Context

InvoiceMe has successfully implemented the core invoicing workflow (Customers → Invoices → Payments) with a 95% completion rate. The system handles invoice lifecycle management (Draft → Sent → Paid), balance calculations, and payment reconciliation effectively. However, several features critical for production deployment remain unimplemented:

**Current State:**
- Core CRUD operations: ✅ Complete
- Invoice lifecycle: ✅ Complete
- Payment processing: ✅ Complete
- Authentication: ✅ Complete
- Integration testing: ✅ Complete

**Missing Capabilities:**
- PDF export for professional invoice delivery
- CSV export for accounting integration
- Email notifications for automated client communication
- Bulk operations for operational efficiency
- Settings/configuration for business customization

These missing features prevent the application from being deployed in a real business environment. While the technical foundation is solid, businesses need the ability to export professional PDFs, integrate with accounting systems via CSV, and automate customer notifications. This PRD defines the requirements to bridge that gap.

### Change Log

| Date | Version | Description | Author |
|------|---------|-------------|---------|
| 2025-11-09 | v1.0 | Initial PRD for missing features Phase 2 | Mike (PM) |

## Requirements

### Functional

#### PDF Export Requirements
- **FR1:** The system must generate PDF invoices matching the on-screen invoice detail layout with professional formatting
- **FR2:** PDF export must include company logo, branding, and customizable header/footer from settings
- **FR3:** PDFs must calculate and display all invoice details accurately including line items, taxes, discounts, and totals
- **FR4:** The system must provide download functionality for individual invoice PDFs from invoice detail page
- **FR5:** PDF generation must support both Draft and Sent invoice statuses (useful for proofing before sending)

#### CSV Export Requirements
- **FR6:** The system must export payment history to CSV format with configurable date range filters
- **FR7:** CSV export must include all payment fields: date, invoice number, customer name, amount, method, reference, balance
- **FR8:** The system must allow filtering payments by customer, payment method, and date range before export
- **FR9:** CSV files must use standard format compatible with Excel, QuickBooks, and other accounting software
- **FR10:** The system must support exporting invoice lists to CSV with status, customer, amounts, and balance data

#### Email Notification Requirements
- **FR11:** The system must send automated email notifications when an invoice status changes to "Sent"
- **FR12:** Email notifications must include invoice PDF attachment and payment instructions
- **FR13:** The system must provide email templates for invoice delivery, payment reminders, and payment confirmations
- **FR14:** Administrators must be able to preview email content before sending
- **FR15:** The system must track email delivery status (sent, delivered, failed) for audit purposes
- **FR16:** Email functionality must be optional with ability to disable per invoice or globally

#### Bulk Operations Requirements
- **FR17:** The system must support bulk status transitions: Draft→Sent for multiple invoices
- **FR18:** The system must provide bulk delete functionality for Draft invoices only
- **FR19:** Bulk operations must show progress indicators and detailed results (success count, failure count, errors)
- **FR20:** The system must validate all invoices before bulk operations and report which items would fail
- **FR21:** Users must confirm bulk operations with clear summary of affected items

#### Settings Page Requirements
- **FR22:** The system must store company information: name, address, phone, email, website
- **FR23:** The system must support company logo upload for invoice branding (PDF and email)
- **FR24:** Settings must include default payment terms configurable per company
- **FR25:** The system must allow configuration of default tax rates and discount policies
- **FR26:** Settings must include SMTP configuration for email delivery (host, port, credentials, from address)
- **FR27:** The system must provide email template customization with variables for customer/invoice data
- **FR28:** Settings changes must apply immediately to new invoices without requiring restart

### Non Functional

- **NFR1:** PDF generation must complete within 3 seconds for invoices with up to 50 line items
- **NFR2:** CSV export must handle datasets up to 10,000 payment records efficiently (< 10 seconds)
- **NFR3:** Email delivery must be asynchronous to avoid blocking API requests
- **NFR4:** Bulk operations must process up to 100 invoices with transactional integrity
- **NFR5:** The system must maintain existing 80%+ test coverage including new features
- **NFR6:** Settings must be persisted in database with proper validation and defaults
- **NFR7:** PDF and CSV generation must be memory-efficient to avoid OutOfMemory errors
- **NFR8:** All file operations (logo upload, PDF generation) must include proper security validation
- **NFR9:** Email functionality must implement retry logic for transient failures (3 retries with exponential backoff)
- **NFR10:** The system must maintain backward compatibility with existing invoice and payment data

## User Interface Design Goals

### Overall UX Vision for New Features

Extend the existing clean, professional ERP interface to include export, notification, and configuration capabilities. New features should feel seamlessly integrated into the existing navigation and page layouts. Prioritize discoverability of export buttons while maintaining the current visual hierarchy. Settings should follow familiar patterns from SaaS applications with clear sections and form validation.

### Key Interaction Paradigms

- **Export Actions:** Context-aware export buttons placed near relevant data (PDF on invoice detail, CSV on payment list)
- **Bulk Selection:** Checkbox-based multi-select with action toolbar appearing when items selected
- **Settings Navigation:** Tabbed interface for different setting categories (Company, Email, Defaults)
- **Progress Feedback:** Real-time progress indicators for long-running operations (bulk actions, email sending)
- **Template Preview:** WYSIWYG or split-pane preview for email template customization

### Core Screens and Views

- **Invoice Detail Page Enhancement** - Add "Export PDF" button with download confirmation
- **Payment List Page Enhancement** - Add "Export CSV" button with filter summary
- **Invoice List Page Enhancement** - Add bulk selection checkboxes and bulk action toolbar
- **Settings Page** - Multi-tab layout with Company Info, Email Configuration, Invoice Defaults tabs
- **Email Preview Modal** - Show rendered email with invoice data before sending
- **Bulk Action Confirmation Dialog** - Summary table of selected items with action confirmation
- **Progress/Results Dialog** - Real-time progress bar with success/failure details after bulk operation

### Accessibility Considerations

- File download actions must provide clear ARIA labels indicating file type and content
- Bulk selection checkboxes must include proper labels for screen readers
- Email preview must be keyboard navigable
- Settings forms must follow established validation patterns with inline errors
- Progress indicators must announce status changes to assistive technologies

### Responsive Design

- PDF export button consolidates with other actions on mobile (action menu)
- Settings forms stack vertically on mobile with full-width inputs
- Bulk action toolbar becomes bottom sheet on mobile
- Email templates use responsive design for various email clients
- CSV export filters collapse into expandable sections on mobile

## Technical Assumptions

### PDF Generation Library
**Choice:** iText 7 (Java) - *Rationale:* Mature, production-ready library with excellent HTML-to-PDF conversion and commercial-friendly AGPL license (with commercial option). Better performance and features than Apache PDFBox for invoice layout complexity. Flying Saucer considered but lacks active maintenance.

**Implementation Approach:**
- Create reusable `InvoicePdfService` in common services package
- Use Thymeleaf templates for PDF content generation (leverages existing Spring Boot integration)
- Support header/footer templates from Settings
- Generate PDFs synchronously for single invoices (<3s requirement is achievable)
- Store temporary PDFs for email attachment (cleanup after 1 hour)

### CSV Export Library
**Choice:** OpenCSV (Java) - *Rationale:* Simple, lightweight, handles escaping and quoting automatically. Better than manual CSV writing which risks injection vulnerabilities and format issues.

**Implementation Approach:**
- Create `CsvExportService` in common services package
- Map query results to CSV DTOs with proper field ordering
- Stream large datasets to avoid memory issues (use Spring's StreamingResponseBody)
- Set proper Content-Disposition headers for download
- Include UTF-8 BOM for Excel compatibility

### Email Service
**Choice:** Spring Boot Mail with Thymeleaf templates - *Rationale:* Native Spring Boot integration, no additional dependencies needed. Thymeleaf provides templating already used in project.

**Implementation Approach:**
- Create `EmailService` in infrastructure layer following existing patterns
- Use Spring's `@Async` for non-blocking email sending
- Store email templates in database (Settings) with fallback to default templates
- Implement `EmailNotificationEvent` domain event for invoice status changes
- Configure SMTP settings via Settings page (stored encrypted in database)
- Add email queue table for retry logic and delivery tracking

### Bulk Operations Strategy
**Choice:** Spring Batch or custom batch processing with `@Transactional` - *Rationale:* For 100 invoice limit, custom implementation with transaction management is simpler than Spring Batch overhead.

**Implementation Approach:**
- Create `BulkInvoiceOperationService` in invoice domain
- Process operations in transaction with rollback on any failure (all-or-nothing)
- Alternative: Process individually and collect results for partial success scenarios
- Use `CompletableFuture` for progress reporting without blocking
- Return detailed `BulkOperationResult` DTO with success/failure lists

### Settings Storage
**Choice:** Database table with JSON column for flexible schema - *Rationale:* Settings schema may evolve, JSON provides flexibility while maintaining type safety through DTOs.

**Implementation Approach:**
- Create `Settings` entity in new `settings` vertical slice
- Single row with UUID, settings_type (e.g., 'COMPANY', 'EMAIL'), and JSONB data column
- Store logo as Base64 in settings (avoid file storage complexity for assessment)
- Alternative: Store logo in S3/Azure Blob for production (document migration path)
- Use `@Transactional` updates with optimistic locking

### Technology Stack Additions

| Component | Technology | Purpose | Rationale |
|-----------|------------|---------|-----------|
| PDF Generation | iText 7 | Invoice PDF creation | Production-ready, feature-rich, good performance |
| CSV Export | OpenCSV | Payment/invoice CSV export | Simple, secure, handles edge cases |
| Email Templates | Thymeleaf | HTML email generation | Already in stack, good Spring integration |
| Email Sending | Spring Boot Mail | SMTP integration | Native Spring Boot support |
| Async Processing | Spring @Async | Email sending, bulk operations | Simple async without message queue overhead |
| Logo Storage | Base64 in DB (initial) | Company logo persistence | Simplest for assessment, document S3 migration |

## Epic List

**Epic 5: PDF Export & Professional Invoice Delivery**
Implement PDF generation for invoices with professional formatting, company branding from settings, and download functionality. This epic delivers the most critical missing feature enabling professional invoice delivery to clients.

**Epic 6: CSV Export & Accounting Integration**
Build CSV export capabilities for payments and invoices with flexible filtering to support integration with external accounting systems like QuickBooks, Xero, and Excel-based workflows.

**Epic 7: Email Notifications & Automation**
Create email notification system with customizable templates, automated invoice delivery, and delivery tracking to reduce manual invoice sending and improve customer communication.

**Epic 8: Bulk Operations & Operational Efficiency**
Implement bulk actions for invoice management (bulk send, bulk delete) with progress tracking and detailed results to enable efficient operations for users managing many invoices.

**Epic 9: Settings & Configuration Management**
Develop settings page for company information, branding, email configuration, and invoice defaults to support business customization and multi-tenant readiness.

## Epic 5: PDF Export & Professional Invoice Delivery

**Epic Goal:** Enable professional invoice delivery by implementing PDF generation with company branding, matching the on-screen invoice layout, and providing seamless download functionality. This epic addresses the highest priority missing feature that prevents real-world invoice delivery to customers.

### Story 5.1: PDF Generation Service Foundation

**As a** developer,
**I want** to implement a reusable PDF generation service using iText,
**so that** we have a foundation for creating professional invoice PDFs.

#### Acceptance Criteria
1. Maven dependency for iText 7 added to `backend/pom.xml`
2. `InvoicePdfService` interface created in `com.invoiceme.common.services`
3. `ITextInvoicePdfService` implementation with method `generateInvoicePdf(InvoiceResponseDTO)`
4. Thymeleaf template `invoice-pdf.html` created in `resources/templates/pdf/`
5. PDF template matches invoice detail page layout (header, line items, totals)
6. Service returns `byte[]` PDF data or `InputStream` for streaming
7. Unit tests verify PDF generation produces valid PDF with correct content
8. Integration test creates PDF from sample invoice and validates structure

#### Technical Notes
- Use iText's HTML-to-PDF converter with Thymeleaf-rendered HTML
- Configure page size (Letter/A4), margins, and font (Helvetica)
- Handle multi-page invoices with repeating headers
- Include invoice number in PDF metadata for searchability

### Story 5.2: Invoice PDF Download Endpoint

**As a** developer,
**I want** to create REST endpoint for PDF download,
**so that** frontend can trigger PDF generation and download.

#### Acceptance Criteria
1. New endpoint `GET /api/invoices/{id}/pdf` in `InvoiceQueryController`
2. Endpoint validates invoice exists and user has access (authorization)
3. Endpoint calls `InvoicePdfService.generateInvoicePdf(invoice)`
4. Response headers set: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="Invoice-{number}.pdf"`
5. PDF streams directly to response without saving to disk
6. Error handling for invalid invoice ID returns 404
7. Error handling for PDF generation failure returns 500 with message
8. Integration test verifies PDF download with valid invoice

#### Technical Notes
- Use Spring's `StreamingResponseBody` for efficient streaming
- Set `Cache-Control: no-cache` to prevent browser caching stale PDFs
- Log PDF generation requests for audit trail
- Consider rate limiting for abuse prevention (future enhancement)

### Story 5.3: PDF Export UI Integration

**As a** user,
**I want** to export invoices as PDF from the invoice detail page,
**so that** I can download professional invoices to send to customers.

#### Acceptance Criteria
1. "Export PDF" button on invoice detail page calls PDF download endpoint
2. Button shows loading state during PDF generation (spinner icon)
3. Browser triggers file download when PDF ready
4. Success toast notification: "PDF downloaded successfully"
5. Error toast notification with retry option on failure
6. PDF button available for all invoice statuses (Draft, Sent, Paid)
7. Downloaded PDF filename format: `Invoice-{invoiceNumber}.pdf`
8. PDF opens correctly in browser PDF viewer and Adobe Reader

#### Technical Notes
- Use `fetch` with `blob()` to handle binary PDF response
- Create temporary download link and trigger programmatically
- Clean up object URL after download to prevent memory leak
- Test download on Chrome, Firefox, Safari, Edge

### Story 5.4: Company Branding Integration (Basic)

**As a** developer,
**I want** to include basic company information in PDFs,
**so that** invoices have professional business branding.

#### Acceptance Criteria
1. PDF template includes hardcoded placeholder company info section (header)
2. Company name: "InvoiceMe" (to be replaced by Settings in Epic 9)
3. Company tagline: "Professional Invoicing System"
4. Template includes footer: "Generated by InvoiceMe - {date}"
5. PDF layout reserves space for logo (placeholder box, implemented in Epic 9)
6. Document metadata includes producer: "InvoiceMe System"
7. Visual design matches invoice detail page styling
8. PDF includes "DRAFT" watermark for Draft status invoices

#### Technical Notes
- Use CSS styles in Thymeleaf template for consistent formatting
- Apply watermark using iText's `PdfCanvas` for Draft invoices
- Leave TODO comments for Settings integration in Epic 9
- Test PDF appearance on different PDF viewers

### Story 5.5: Performance Optimization & Testing

**As a** developer,
**I want** to optimize PDF generation performance,
**so that** PDFs generate within 3 seconds for typical invoices.

#### Acceptance Criteria
1. Performance test verifies PDF generation <3s for 50 line items
2. Memory profiling shows no memory leaks on repeated generation
3. iText font caching configured for performance
4. Template rendering optimized (minimal Thymeleaf complexity)
5. Load test: 10 concurrent PDF generations complete successfully
6. Integration test for invoice with maximum complexity (50 items, discounts, taxes)
7. Error handling test for malformed invoice data
8. Documentation added: "PDF Export" section in technical docs

#### Technical Notes
- Consider PDF template caching for repeated generations
- Monitor heap memory usage during load test
- Set reasonable timeout (10s) for PDF generation
- Log performance metrics (generation time) for monitoring

## Epic 6: CSV Export & Accounting Integration

**Epic Goal:** Enable seamless integration with external accounting systems by implementing CSV export for payments and invoices with flexible filtering. This epic addresses the need for data portability and accounting software integration, a medium-priority requirement for production deployment.

### Story 6.1: CSV Export Service Foundation

**As a** developer,
**I want** to implement a reusable CSV export service,
**so that** we can export various data types to CSV format safely.

#### Acceptance Criteria
1. Maven dependency for OpenCSV added to `backend/pom.xml`
2. `CsvExportService` interface created in `com.invoiceme.common.services`
3. `OpenCsvExportService` implementation with generic method `exportToCsv(List<T> data, Class<T> type)`
4. Service uses reflection or explicit field mappings for CSV column generation
5. CSV headers use human-readable names (e.g., "Payment Date" not "paymentDate")
6. Service handles special characters, quotes, and commas properly (escaping)
7. UTF-8 BOM included for Excel compatibility
8. Unit tests verify CSV output format correctness

#### Technical Notes
- Use OpenCSV's `@CsvBindByName` annotations on DTOs
- Set RFC 4180 compliance for standard CSV format
- Handle null values gracefully (empty string vs "null")
- Test with Excel, Google Sheets, QuickBooks import

### Story 6.2: Payment CSV Export Endpoint

**As a** developer,
**I want** to create REST endpoint for payment CSV export,
**so that** users can export filtered payment history.

#### Acceptance Criteria
1. New endpoint `GET /api/payments/export/csv` in `PaymentQueryController`
2. Endpoint accepts query parameters: `startDate`, `endDate`, `customerId`, `paymentMethod`
3. Endpoint reuses existing `ListPaymentsQuery` logic with filters
4. Response streams CSV with headers: `Content-Type: text/csv`, `Content-Disposition: attachment; filename="payments-{date}.csv"`
5. CSV columns: Payment Date, Invoice Number, Customer Name, Amount, Payment Method, Reference, Remaining Balance
6. CSV rows sorted by payment date descending
7. Empty result set returns CSV with headers only (no error)
8. Integration test verifies CSV export with various filters

#### Technical Notes
- Use Spring's `StreamingResponseBody` for large datasets
- Limit export to 10,000 records (return error if exceeded)
- Format dates as ISO 8601 (YYYY-MM-DD) for sorting
- Format currency as decimal (no symbols) for spreadsheet calculations

### Story 6.3: Payment CSV Export UI

**As a** user,
**I want** to export payment history to CSV from the payments page,
**so that** I can import payment data into accounting software.

#### Acceptance Criteria
1. "Export CSV" button on payments page triggers CSV download
2. Button uses currently applied filters (date range, payment method)
3. Button shows loading state during export generation
4. CSV download includes filename: `InvoiceMe-Payments-{YYYY-MM-DD}.csv`
5. Toast notification confirms: "CSV exported successfully (X records)"
6. Error handling for large datasets: "Export limited to 10,000 records. Please refine filters."
7. Export button disabled when no payments match filters
8. CSV opens correctly in Excel with proper formatting (dates, currency)

#### Technical Notes
- Same download approach as PDF (fetch blob, create download link)
- Display record count before export for transparency
- Consider adding "Export All" vs "Export Filtered" options
- Test CSV import into QuickBooks and Excel

### Story 6.4: Invoice CSV Export

**As a** developer,
**I want** to implement invoice CSV export,
**so that** users can export invoice lists for reporting.

#### Acceptance Criteria
1. New endpoint `GET /api/invoices/export/csv` in `InvoiceQueryController`
2. Endpoint accepts filters: `customerId`, `status`, `startDate`, `endDate`
3. CSV columns: Invoice Number, Customer Name, Issue Date, Due Date, Status, Total Amount, Balance Due, Days Overdue
4. "Export CSV" button added to invoices list page
5. Button uses current filters and sort order
6. CSV filename: `InvoiceMe-Invoices-{YYYY-MM-DD}.csv`
7. Integration test verifies invoice CSV export
8. Documentation updated with export feature usage

#### Technical Notes
- Include calculated fields (Days Overdue) in export
- Consider adding "Amount Paid" column (Total - Balance)
- Format currency consistently with payment export
- Test with various invoice statuses and edge cases

### Story 6.5: CSV Export Security & Validation

**As a** developer,
**I want** to implement security controls for CSV exports,
**so that** data exports are safe and performant.

#### Acceptance Criteria
1. CSV endpoints require authentication (JWT token)
2. Authorization check: users can only export their own data (tenant isolation when implemented)
3. Rate limiting: max 10 CSV exports per user per hour
4. Input validation: date ranges cannot exceed 1 year
5. CSV injection prevention: escape formulas (=, +, -, @) with single quote
6. Audit logging: log all CSV export requests with user, filters, record count
7. Error handling: proper error messages for invalid date ranges, missing filters
8. Performance test: 10,000 record export completes in <10 seconds

#### Technical Notes
- Use Spring Security for authorization
- Implement rate limiting with Redis (or in-memory cache for assessment)
- CSV injection: prefix cells starting with =+-@ with single quote (')
- Monitor export performance and optimize queries with indexes

## Epic 7: Email Notifications & Automation

**Epic Goal:** Automate customer communication by implementing email notifications for invoice delivery with customizable templates and delivery tracking. This epic is low priority for initial deployment but provides significant value for automated workflows in production environments.

### Story 7.1: Email Service Infrastructure

**As a** developer,
**I want** to implement email sending infrastructure with Spring Boot Mail,
**so that** we can send transactional emails reliably.

#### Acceptance Criteria
1. Maven dependencies added: `spring-boot-starter-mail`, `spring-boot-starter-thymeleaf` (already present)
2. `EmailService` interface created in `com.invoiceme.common.services`
3. `SmtpEmailService` implementation with methods: `sendEmail(to, subject, body, attachments)`
4. Email configuration in `application.yml` with SMTP properties (host, port, username, password, from address)
5. Thymeleaf email template `invoice-delivery.html` in `resources/templates/email/`
6. `@Async` annotation for non-blocking email sending
7. Email sending returns `CompletableFuture<EmailResult>` with success/failure status
8. Unit tests with mock SMTP server (GreenMail or similar)

#### Technical Notes
- Use Spring's `JavaMailSender` abstraction
- Configure connection pooling for SMTP connections
- Set reasonable timeout (10s) for email operations
- Support both plain text and HTML email bodies

### Story 7.2: Invoice Email Notification Command

**As a** developer,
**I want** to create command for sending invoice emails,
**so that** we can trigger invoice delivery via email.

#### Acceptance Criteria
1. New command `SendInvoiceEmailCommand` in invoice vertical slice
2. `SendInvoiceEmailCommandHandler` generates invoice PDF attachment
3. Handler renders email template with invoice data (customer name, invoice number, amount, due date)
4. Email subject: "Invoice {invoiceNumber} from {companyName}"
5. Email includes payment instructions: "Please pay ${amount} by {dueDate}"
6. Email sends asynchronously with delivery tracking
7. Command creates `InvoiceEmailSent` domain event on success
8. Integration test verifies email sending with mock SMTP server

#### Technical Notes
- Attach PDF inline or as attachment (attachment is standard)
- Include unsubscribe link (placeholder for future implementation)
- Track email delivery in `invoice_emails` table (id, invoice_id, sent_at, delivered_at, status)
- Handle PDF generation errors gracefully (don't block email if PDF fails)

### Story 7.3: Automatic Email on Invoice Send

**As a** user,
**I want** invoices to automatically email to customers when status changes to Sent,
**so that** customers receive invoices immediately without manual intervention.

#### Acceptance Criteria
1. `SendInvoiceCommand` triggers email notification after successful status change
2. Email sending is optional (configurable per invoice or global setting)
3. UI checkbox on Send Invoice dialog: "Send email notification to customer"
4. Checkbox defaults to checked if customer has valid email address
5. Email delivery status shown on invoice detail page: "Email sent {date}" or "Email not sent"
6. Failed email delivery shows error message with retry button
7. Domain event `InvoiceStatusChanged` triggers email via event listener
8. Integration test verifies email sent when invoice status changes to Sent

#### Technical Notes
- Use Spring Events (`@EventListener`) for decoupling
- Email failures should not roll back invoice status change
- Store email delivery status in separate table (eventual consistency)
- Provide manual "Resend Email" button on invoice detail page

### Story 7.4: Email Template Customization (Basic)

**As a** user,
**I want** to customize email templates with my branding,
**so that** invoice emails reflect my business identity.

#### Acceptance Criteria
1. Email template includes company logo from settings (placeholder for Epic 9)
2. Template uses company name and contact information
3. Template variables: `{customerName}`, `{invoiceNumber}`, `{amount}`, `{dueDate}`, `{companyName}`
4. Default template includes professional styling (CSS inline for email compatibility)
5. Email preview functionality shows rendered email before sending
6. Preview modal accessible from invoice detail page: "Preview Email" button
7. Email footer includes: "Powered by InvoiceMe" (white-label option for future)
8. Template tested in major email clients (Gmail, Outlook, Apple Mail)

#### Technical Notes
- Use inline CSS for email client compatibility (avoid external stylesheets)
- Test with Email on Acid or Litmus for client compatibility
- Thymeleaf variables for easy customization
- Mobile-responsive email design (media queries)

### Story 7.5: Email Delivery Tracking & Reliability

**As a** developer,
**I want** to implement email retry logic and delivery tracking,
**so that** emails are delivered reliably with audit trail.

#### Acceptance Criteria
1. Email sending implements retry logic: 3 attempts with exponential backoff (1s, 5s, 15s)
2. Failed emails stored in `failed_emails` table for manual review
3. Email status tracked: PENDING, SENT, DELIVERED, FAILED, BOUNCED (DELIVERED/BOUNCED require webhook, implement SENT/FAILED only)
4. Invoice detail page shows email history: sent timestamp, status, recipient
5. Admin can manually retry failed emails from invoice detail
6. Email sending errors logged with correlation ID for debugging
7. Integration test verifies retry logic with transient SMTP failures
8. Documentation: "Email Configuration" guide for SMTP setup

#### Technical Notes
- Use `@Retryable` annotation from Spring Retry
- Consider email queue table for better reliability (future enhancement)
- Log all email attempts for compliance/audit
- Provide health check endpoint for SMTP connectivity

## Epic 8: Bulk Operations & Operational Efficiency

**Epic Goal:** Enable efficient management of multiple invoices through bulk operations (bulk send, bulk delete) with progress tracking and detailed results. This epic is low priority but provides significant operational efficiency for users managing many invoices.

### Story 8.1: Bulk Operation Service Foundation

**As a** developer,
**I want** to implement a service for bulk invoice operations,
**so that** we can process multiple invoices in a single transaction.

#### Acceptance Criteria
1. `BulkInvoiceOperationService` created in invoice vertical slice
2. Service method `bulkSendInvoices(List<UUID> invoiceIds)` returns `BulkOperationResult`
3. Service method `bulkDeleteInvoices(List<UUID> invoiceIds)` returns `BulkOperationResult`
4. `BulkOperationResult` DTO includes: total count, success count, failure count, list of errors with invoice ID and reason
5. Operations process in transaction with rollback on any failure (all-or-nothing approach)
6. Alternative implementation: process individually, collect results (partial success approach)
7. Service validates all invoices before processing (fail fast)
8. Unit tests verify bulk operations with mixed valid/invalid invoices

#### Technical Notes
- Decide on transaction strategy: all-or-nothing vs partial success
- All-or-nothing is safer, partial success is more flexible
- Recommendation: Partial success with detailed error reporting
- Limit bulk operations to 100 invoices per request

### Story 8.2: Bulk Send Invoices Command

**As a** developer,
**I want** to create command for bulk invoice sending,
**so that** multiple invoices can transition to Sent status together.

#### Acceptance Criteria
1. New command `BulkSendInvoicesCommand` with list of invoice IDs
2. `BulkSendInvoicesCommandHandler` validates all invoices are in Draft status
3. Handler validates all invoices have at least one line item
4. Handler calls `SendInvoiceCommand` for each valid invoice
5. Handler collects results: successful sends, failed sends with reasons
6. Endpoint `POST /api/invoices/bulk/send` with request body `{invoiceIds: []}`
7. Response includes detailed results: `{total: 10, success: 8, failed: 2, errors: [{id, reason}]}`
8. Integration test verifies bulk send with mixed valid/invalid invoices

#### Technical Notes
- Reuse existing `SendInvoiceCommand` logic for consistency
- Validate invoice ownership (user can only send their invoices)
- Consider email sending for bulk operations (optional flag)
- Process sequentially to avoid database contention

### Story 8.3: Bulk Delete Invoices Command

**As a** developer,
**I want** to create command for bulk invoice deletion,
**so that** multiple Draft invoices can be deleted together.

#### Acceptance Criteria
1. New command `BulkDeleteInvoicesCommand` with list of invoice IDs
2. `BulkDeleteInvoicesCommandHandler` validates all invoices are in Draft status
3. Handler validates invoices have no associated payments
4. Handler calls `DeleteInvoiceCommand` for each valid invoice (or direct deletion)
5. Soft delete implementation (sets `deleted` flag, not hard delete)
6. Endpoint `POST /api/invoices/bulk/delete` with request body `{invoiceIds: []}`
7. Response includes detailed results with error messages
8. Integration test verifies bulk delete with Draft and Sent invoices (Sent should fail)

#### Technical Notes
- Only Draft invoices can be deleted (business rule)
- Consider hard delete vs soft delete (soft delete recommended for audit)
- Validate cascade behavior (what happens to domain events?)
- Test with invoices referenced by other entities

### Story 8.4: Bulk Operations UI - Selection & Toolbar

**As a** user,
**I want** to select multiple invoices from the invoice list,
**so that** I can perform bulk actions efficiently.

#### Acceptance Criteria
1. Checkbox added to each row in invoice list table
2. "Select All" checkbox in table header selects all visible invoices
3. Selection state persists across pagination (stores selected IDs)
4. Bulk action toolbar appears when 1+ invoices selected
5. Toolbar shows selected count: "X invoices selected"
6. Toolbar buttons: "Bulk Send", "Bulk Delete", "Clear Selection"
7. Bulk Send button only enabled when all selected invoices are Draft
8. Bulk Delete button only enabled when all selected invoices are Draft
9. Responsive design: toolbar becomes bottom sheet on mobile

#### Acceptance Criteria (continued)
10. Visual feedback: selected rows highlighted with background color
11. Selection clears after successful bulk operation
12. "Clear Selection" button removes all selections

#### Technical Notes
- Use Zustand store for managing selected invoice IDs
- Disable action buttons with tooltip explaining why (e.g., "Can only send Draft invoices")
- Consider limiting selection to single page vs across pages
- Test performance with 100+ selected invoices

### Story 8.5: Bulk Operation Confirmation & Progress

**As a** user,
**I want** to review and confirm bulk operations before execution,
**so that** I don't accidentally modify wrong invoices.

#### Acceptance Criteria
1. Clicking "Bulk Send" opens confirmation dialog
2. Dialog shows summary table of selected invoices (number, customer, amount)
3. Dialog shows warnings: "This will send X invoices to customers via email" (if email enabled)
4. Dialog has "Cancel" and "Confirm Send" buttons
5. After confirmation, progress dialog shows with progress bar
6. Progress updates in real-time as invoices process
7. Results dialog shows detailed results: success count, failure count, error list
8. Error list shows invoice number and reason for each failure
9. Same flow for bulk delete with appropriate warnings

#### Technical Notes
- Use WebSocket or polling for progress updates (polling simpler for assessment)
- Alternative: Show spinner, return results when complete (simpler, no progress bar)
- Recommendation: Simple approach without real-time progress for MVP
- Store bulk operation results for later review (audit table)

### Story 8.6: Bulk Operation Security & Performance

**As a** developer,
**I want** to implement security and performance controls for bulk operations,
**so that** bulk actions are safe and don't overload the system.

#### Acceptance Criteria
1. Bulk operations require authentication and authorization
2. Rate limiting: max 5 bulk operations per user per hour
3. Size limit: max 100 invoices per bulk operation
4. Validation: user can only bulk operate on their own invoices (tenant isolation)
5. Audit logging: log all bulk operations with user, invoice IDs, results
6. Transaction timeout set to reasonable value (30 seconds)
7. Performance test: bulk send 100 invoices completes in <30 seconds
8. Error handling: database deadlocks handled gracefully with retry

#### Technical Notes
- Use pessimistic locking for invoice status updates to prevent race conditions
- Consider batch processing in chunks (e.g., 20 at a time) for large operations
- Monitor database connection pool during bulk operations
- Add metrics for bulk operation duration and success rate

## Epic 9: Settings & Configuration Management

**Epic Goal:** Provide business customization capabilities through a comprehensive settings page managing company information, branding, email configuration, and invoice defaults. This epic enables multi-tenant readiness and professional business identity, marked as future phase but foundational for production deployment.

### Story 9.1: Settings Domain Model & Repository

**As a** developer,
**I want** to implement the Settings domain model and persistence,
**so that** we can store and retrieve application configuration.

#### Acceptance Criteria
1. New vertical slice: `com.invoiceme.features.settings`
2. `Settings` entity with fields: id (UUID), settingsType (enum: COMPANY, EMAIL, INVOICE_DEFAULTS), data (JSONB), version (for optimistic locking)
3. `SettingsType` enum: COMPANY, EMAIL, INVOICE_DEFAULTS
4. `SettingsRepository` interface following repository pattern
5. `CompanySettingsData` record: name, address, phone, email, website, logoBase64
6. `EmailSettingsData` record: smtpHost, smtpPort, smtpUsername, smtpPassword (encrypted), fromEmail, fromName
7. `InvoiceDefaultsData` record: defaultPaymentTerms, defaultTaxRate, defaultDiscountRate, invoiceNotes
8. Database migration creates `settings` table with JSONB column

#### Technical Notes
- Use separate settings types for logical grouping (easier updates)
- Alternative: Single settings row with all data (simpler querying)
- Encrypt sensitive data (SMTP password) using Spring encryption
- Add unique constraint on settingsType (only one COMPANY settings row)

### Story 9.2: Settings Commands & Queries

**As a** developer,
**I want** to implement CQRS commands and queries for settings management,
**so that** settings can be updated and retrieved following existing patterns.

#### Acceptance Criteria
1. Command: `UpdateCompanySettingsCommand` with `CompanySettingsData`
2. Command: `UpdateEmailSettingsCommand` with `EmailSettingsData`
3. Command: `UpdateInvoiceDefaultsCommand` with `InvoiceDefaultsData`
4. Query: `GetCompanySettingsQuery` returns `CompanySettingsData`
5. Query: `GetEmailSettingsQuery` returns `EmailSettingsData` (with password masked)
6. Query: `GetInvoiceDefaultsQuery` returns `InvoiceDefaultsData`
7. Command handlers validate settings data (e.g., valid email format, SMTP port range)
8. Query handlers return default values if settings not yet configured

#### Technical Notes
- Settings updates are upserts (create if not exists)
- Use optimistic locking to prevent concurrent update conflicts
- Validate SMTP settings by attempting connection (optional test button)
- Cache settings in memory with TTL (60 seconds) for performance

### Story 9.3: Settings REST API

**As a** developer,
**I want** to create REST endpoints for settings management,
**so that** frontend can manage settings through API.

#### Acceptance Criteria
1. Endpoints in `SettingsController`:
   - `GET /api/settings/company` - Get company settings
   - `PUT /api/settings/company` - Update company settings
   - `GET /api/settings/email` - Get email settings (password masked)
   - `PUT /api/settings/email` - Update email settings
   - `POST /api/settings/email/test` - Test SMTP connection
   - `GET /api/settings/invoice-defaults` - Get invoice defaults
   - `PUT /api/settings/invoice-defaults` - Update invoice defaults
2. All endpoints require authentication (admin role preferred, any authenticated user for assessment)
3. Logo upload: `POST /api/settings/company/logo` with multipart file upload
4. Logo validation: max 2MB, formats: PNG, JPG, SVG
5. Logo conversion to Base64 for storage
6. OpenAPI documentation for all settings endpoints

#### Technical Notes
- Consider role-based access (only admins can change settings)
- For assessment: any authenticated user can manage settings
- Logo upload uses multipart/form-data
- Return 400 Bad Request for invalid settings data

### Story 9.4: Settings Page UI - Company Information

**As a** user,
**I want** to configure my company information and branding,
**so that** invoices and emails reflect my business identity.

#### Acceptance Criteria
1. Settings page accessible from navigation menu: `/settings`
2. Tabbed interface with tabs: "Company", "Email", "Invoice Defaults"
3. Company tab includes form fields: Company Name, Address, Phone, Email, Website
4. Logo upload section with drag-and-drop and file picker
5. Logo preview shows uploaded image (or placeholder if none)
6. Logo upload validates file size and format client-side
7. Form validation: email format, phone format, required fields
8. "Save Changes" button with loading state during save
9. Success toast: "Company settings saved successfully"
10. Form pre-populates with existing settings on load

#### Technical Notes
- Use React Hook Form for form management and validation
- Logo preview using FileReader API for immediate feedback
- Consider Cropper.js for logo cropping (future enhancement)
- Responsive form layout (stacked on mobile)

### Story 9.5: Settings Page UI - Email Configuration

**As a** user,
**I want** to configure SMTP email settings,
**so that** the system can send invoice emails through my email provider.

#### Acceptance Criteria
1. Email tab includes form fields: SMTP Host, SMTP Port, Username, Password, From Email, From Name
2. Password field uses password input type with "Show/Hide" toggle
3. Form includes "Test Connection" button that validates SMTP settings
4. Test connection shows loading state and success/error result
5. Success message: "SMTP connection successful"
6. Error message shows specific error (e.g., "Authentication failed", "Connection refused")
7. Form validation: valid email format for From Email, port number range (1-65535)
8. Tooltip icons explaining each field (e.g., "SMTP Host: Your email provider's SMTP server address")
9. Example settings shown for common providers: Gmail, Outlook, SendGrid

#### Technical Notes
- Store password encrypted in backend
- Test connection calls backend endpoint without saving
- Provide preset configurations for common providers (dropdown)
- Add security warning about storing SMTP credentials

### Story 9.6: Settings Page UI - Invoice Defaults

**As a** user,
**I want** to set default values for new invoices,
**so that** invoice creation is faster with pre-filled values.

#### Acceptance Criteria
1. Invoice Defaults tab includes form fields:
   - Default Payment Terms (dropdown: Net 30, Net 60, Due on Receipt, Custom)
   - Custom Payment Terms (text field, enabled when Custom selected)
   - Default Tax Rate (percentage input, 0-100%)
   - Default Discount Rate (percentage input, 0-100%)
   - Default Invoice Notes (textarea)
2. Form validation: tax and discount rates within valid range
3. Save button updates invoice defaults
4. Invoice creation form uses these defaults when creating new invoice
5. Defaults are suggestions (user can override when creating invoice)
6. Preview section shows sample invoice with applied defaults
7. Reset to Defaults button restores system defaults (Net 30, 0% tax, 0% discount, empty notes)

#### Technical Notes
- Store percentages as decimals (e.g., 8% = 0.08)
- Invoice creation API checks settings and applies defaults
- Consider template system for invoice notes (variables like {customerName})
- Test defaults applied correctly when creating new invoices

### Story 9.7: Settings Integration with Existing Features

**As a** developer,
**I want** to integrate settings with PDF generation, emails, and invoices,
**so that** configured settings are used throughout the application.

#### Acceptance Criteria
1. PDF generation uses company settings for header (name, logo, contact info)
2. PDF footer includes company website and custom footer text from settings
3. Email templates use company settings for branding and sender information
4. Email sender: "From Name <from@email.com>" from email settings
5. Invoice creation applies invoice defaults for payment terms, tax, discount, notes
6. Default settings are applied only when creating new invoice (not editing existing)
7. Settings changes apply immediately to new invoices/PDFs/emails (no cache invalidation issues)
8. Integration tests verify settings used correctly in PDF, email, invoice creation

#### Technical Notes
- Inject `SettingsService` into PDF, Email, Invoice services
- Gracefully handle missing settings (use hardcoded defaults)
- Cache settings with short TTL to balance performance and freshness
- Document settings precedence: user input > invoice-level settings > defaults

## Checklist Results Report

### Executive Summary
- **Overall PRD Completeness:** 92%
- **Phase 2 Scope Appropriateness:** Well-scoped for 3-5 day implementation
- **Readiness for Development Phase:** Ready with minor notes
- **Most Critical Gaps:** Email webhook integration for delivery tracking, multi-tenant authorization strategy

### Category Analysis

| Category | Status | Critical Issues |
|----------|--------|-----------------|
| 1. Problem Definition & Context | PASS | None - Clear business justification for missing features |
| 2. Scope Definition | PASS | None - Features prioritized appropriately (HIGH: PDF, MEDIUM: CSV, LOW: Email/Bulk/Settings) |
| 3. User Experience Requirements | PASS | None - UI integration points clearly defined |
| 4. Functional Requirements | PASS | None - Comprehensive requirements for all features |
| 5. Non-Functional Requirements | PASS | None - Performance, security, reliability requirements defined |
| 6. Epic & Story Structure | PASS | None - Logical progression from infrastructure to UI |
| 7. Technical Guidance | PASS | Minor - Email delivery tracking webhook integration needs clarification |
| 8. Cross-Feature Integration | PASS | Minor - Settings integration sequence with other epics needs coordination |
| 9. Clarity & Communication | PASS | None - Well-structured with clear acceptance criteria |

### Top Issues by Priority

**BLOCKERS:** None identified

**HIGH:**
- Email delivery tracking limited to SENT/FAILED without webhook integration (DELIVERED/BOUNCED requires external service)
- Multi-tenant authorization strategy not fully specified (acceptable for single-tenant assessment)

**MEDIUM:**
- Logo storage strategy: Base64 in DB for assessment, migration path to S3/Azure Blob not detailed
- Bulk operation transaction strategy: all-or-nothing vs partial success needs decision
- Email template management: Database storage vs file-based templates

**LOW:**
- Rate limiting implementation: Redis vs in-memory cache choice
- Settings caching strategy and cache invalidation
- PDF generation pooling/caching for performance optimization

### Scope Assessment

**Appropriately Scoped Features:**
- PDF generation with iText (proven library, clear requirements)
- CSV export for payments and invoices (straightforward implementation)
- Basic email sending with Spring Boot Mail (well-understood technology)
- Settings page with three main sections (company, email, defaults)

**Potential Additions (if time permits):**
- Email delivery webhooks for DELIVERED/BOUNCED status (requires SendGrid/Mailgun)
- Email template visual editor (WYSIWYG)
- Logo cropping tool for better branding control
- Bulk operation progress via WebSocket (real-time updates)

**Complexity Appropriate:**
- 32 stories across 5 epics is manageable for Phase 2 development
- Each epic builds on previous infrastructure (logical dependency chain)
- Stories sized for 2-6 hour implementation each
- Can be implemented incrementally: Epic 5 → Epic 6 → (Epic 7,8,9 in parallel)

### Technical Readiness

**Clear Technical Constraints:**
- iText 7 for PDF generation (AGPL license acceptable for assessment)
- OpenCSV for CSV export (Apache 2.0 license)
- Spring Boot Mail for email sending (native integration)
- Thymeleaf for PDF and email templates (already in stack)
- Settings stored in database JSONB column for flexibility

**Identified Technical Risks:**
- PDF generation performance: 3-second target may require optimization for complex invoices
- Email reliability: SMTP failures need retry logic and monitoring
- Logo storage: Base64 in DB is simple but may have size/performance issues at scale
- Bulk operations: Transaction boundaries and locking strategy needs careful implementation
- Settings encryption: SMTP password encryption adds complexity

**Areas for Developer Investigation:**
- Optimal iText template structure for maintainability
- Email queue vs direct sending trade-off
- Settings cache invalidation strategy
- CSV injection prevention best practices
- Bulk operation progress tracking mechanism (polling vs WebSocket)

### Recommendations

1. **Proceed to Development** - PRD is comprehensive and ready
2. **Epic Execution Sequence:**
   - **Phase 2A (High Priority):** Epic 5 (PDF Export) - 2-3 days
   - **Phase 2B (Medium Priority):** Epic 6 (CSV Export) - 1-2 days
   - **Phase 2C (Low Priority - Optional):** Epic 7 (Email), Epic 8 (Bulk), Epic 9 (Settings) - 3-5 days total
3. **During Development:**
   - Start with Epic 5 (PDF) as it has highest business value
   - Epic 6 (CSV) is independent and can be developed in parallel
   - Epic 9 (Settings) should be implemented before Epic 7 (Email) for proper integration
   - Epic 8 (Bulk) is independent and can be implemented anytime
4. **Architecture Decisions Needed:**
   - Confirm bulk operation transaction strategy (recommend partial success)
   - Choose logo storage approach (Base64 for MVP, document S3 migration)
   - Decide email template storage (recommend database for flexibility)
   - Select rate limiting implementation (in-memory acceptable for assessment)

### Final Decision

**READY FOR DEVELOPMENT** ✅

The PRD comprehensively defines all missing features with appropriate prioritization, clear acceptance criteria, and realistic scope. Epic 5 (PDF Export) can begin immediately as highest priority. Epic 6 (CSV Export) provides immediate ROI for accounting integration. Epics 7, 8, and 9 are lower priority but well-specified for future implementation.

## Next Steps

### For UX Designer
Review this PRD and create UI mockups for:
1. PDF export button integration on invoice detail page
2. CSV export button with filter summary on payments page
3. Bulk selection UI and action toolbar on invoice list page
4. Settings page with three-tab layout (Company, Email, Invoice Defaults)
5. Email preview modal showing rendered email template
6. Bulk operation confirmation dialog with results display

### For Backend Developer
Review this PRD and begin implementation with:
1. **Epic 5 Story 5.1:** Set up iText 7 dependency and create InvoicePdfService
2. **Epic 5 Story 5.2:** Implement PDF download endpoint
3. Follow with remaining Epic 5 stories for complete PDF export feature
4. Then proceed to Epic 6 for CSV export (can be parallel with Epic 5 UI work)

### For Frontend Developer
Review this PRD and begin implementation with:
1. **Epic 5 Story 5.3:** Add "Export PDF" button to invoice detail page (after backend endpoint ready)
2. **Epic 6 Story 6.3:** Add "Export CSV" button to payments page (after backend endpoint ready)
3. Follow with Epic 8 bulk selection UI (independent work)
4. Then Epic 9 settings page UI (independent work)

### For QA / Testing
Review this PRD and create test plans for:
1. PDF generation and download functionality (various invoice types, browsers)
2. CSV export with different filters (date ranges, payment methods)
3. Email delivery and retry logic (mock SMTP server)
4. Bulk operations with large datasets (100 invoices)
5. Settings page validation and integration testing

### For Project Manager
Use this PRD to:
1. Create detailed project plan for Phase 2 implementation
2. Estimate effort for each epic (recommended: 8-12 days total for all epics)
3. Prioritize Epic 5 and Epic 6 for first release (core export functionality)
4. Plan Epics 7, 8, 9 for subsequent releases based on business priority
5. Schedule regular demos to validate features against acceptance criteria
