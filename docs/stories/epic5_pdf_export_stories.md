# Epic 5: PDF Export - User Stories

**Epic Goal:** Enable professional invoice delivery by implementing PDF generation with company branding, matching the on-screen invoice layout, and providing seamless download functionality. This epic addresses the highest priority missing feature that prevents real-world invoice delivery to customers.

**Epic Priority:** HIGH - Critical for production deployment
**Estimated Duration:** 2-3 days
**Dependencies:** Core invoice functionality (Complete), Settings infrastructure (Deferred to Epic 9)

---

## Story 5.1: PDF Generation Infrastructure Setup

**User Story:**
As a **developer**,
I want **to set up the PDF generation infrastructure using iText 7**,
so that **we have a reliable, production-ready foundation for creating professional invoice PDFs**.

### Acceptance Criteria

1. **Dependency Configuration**
   - [ ] iText 7 core dependency added to `backend/pom.xml` with correct version (7.2.5 or later)
   - [ ] iText HTML-to-PDF converter module included for template rendering
   - [ ] Maven build completes successfully with no dependency conflicts
   - [ ] License compliance documented (AGPL for assessment, note commercial option for production)

2. **Service Interface Design**
   - [ ] `InvoicePdfService` interface created in `com.invoiceme.common.services` package
   - [ ] Interface defines method signature: `byte[] generateInvoicePdf(InvoiceResponseDTO invoice)`
   - [ ] Alternative streaming method: `InputStream generateInvoicePdfStream(InvoiceResponseDTO invoice)`
   - [ ] Service interface includes JavaDoc with usage examples and error handling documentation

3. **Implementation Class**
   - [ ] `ITextInvoicePdfService` implementation class created implementing `InvoicePdfService`
   - [ ] Service registered as Spring `@Service` component for dependency injection
   - [ ] Constructor injection used for any required dependencies (e.g., template engine)
   - [ ] Error handling wraps iText exceptions in custom `PdfGenerationException`

4. **PDF Configuration**
   - [ ] Page size configured to Letter (8.5" x 11") with fallback to A4 support
   - [ ] Page margins set to professional defaults (1 inch / 2.54 cm on all sides)
   - [ ] Default font configured to Helvetica (or embedded font for Unicode support)
   - [ ] PDF metadata populated: title, author, creator, subject, creation date

5. **Template Foundation**
   - [ ] Thymeleaf template file `invoice-pdf.html` created in `src/main/resources/templates/pdf/`
   - [ ] Template includes basic structure: header, invoice details, line items table, totals section
   - [ ] CSS styles embedded inline for PDF rendering compatibility
   - [ ] Template uses Thymeleaf expressions for dynamic data binding

6. **Content Generation**
   - [ ] Service integrates Thymeleaf engine to render HTML template with invoice data
   - [ ] iText HTML-to-PDF converter transforms rendered HTML to PDF document
   - [ ] Generated PDF includes all invoice information: number, customer, dates, line items, totals
   - [ ] Multi-page support works correctly for invoices with 50+ line items

7. **Unit Testing**
   - [ ] Unit test verifies PDF generation returns valid byte array
   - [ ] Test validates PDF content includes invoice number and customer name
   - [ ] Test confirms PDF is valid format (can be opened by PDF reader)
   - [ ] Test measures performance: generation completes in <3 seconds for 10 line items

8. **Integration Testing**
   - [ ] Integration test creates PDF from complete InvoiceResponseDTO
   - [ ] Test validates PDF structure using iText PdfReader
   - [ ] Test extracts text from PDF and verifies key data fields present
   - [ ] Test validates PDF metadata (title, author, creation date)

### Technical Notes

**iText Configuration:**
- Use iText 7's `HtmlConverter.convertToPdf()` for HTML-to-PDF conversion
- Configure `ConverterProperties` with base URI for resource loading
- Set PDF/A compliance for archival quality (optional but recommended)

**Template Design:**
- Use CSS Grid or Flexbox for layout (limited support, test carefully)
- Avoid complex CSS features not supported by iText's HTML renderer
- Embed images as Base64 data URIs for company logo support
- Use web-safe fonts or embed custom fonts in PDF

**Performance Considerations:**
- Cache Thymeleaf template for reuse across multiple generations
- Reuse iText font objects to avoid repeated parsing
- Stream PDF directly to output for large documents
- Set reasonable timeout (10 seconds) for PDF generation

**Error Handling:**
- Catch `IOException` for template loading failures
- Catch `PdfException` for PDF rendering errors
- Log full stack trace with invoice ID for debugging
- Return meaningful error messages to API consumers

### Testing Requirements

**Unit Tests (JUnit):**
```java
@Test
void generateInvoicePdf_ShouldReturnValidPdfBytes() {
    // Given: Sample invoice DTO
    InvoiceResponseDTO invoice = createSampleInvoice();

    // When: Generate PDF
    byte[] pdfBytes = pdfService.generateInvoicePdf(invoice);

    // Then: PDF is valid and contains invoice data
    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    assertTrue(isPdfValid(pdfBytes));
}
```

**Integration Tests (Spring Boot Test):**
```java
@SpringBootTest
class InvoicePdfServiceIntegrationTest {
    @Autowired
    private InvoicePdfService pdfService;

    @Test
    void generatePdf_ShouldIncludeAllInvoiceData() {
        // Test with real database invoice
        InvoiceResponseDTO invoice = invoiceRepository.findById(testInvoiceId);
        byte[] pdf = pdfService.generateInvoicePdf(invoice);

        String pdfText = extractTextFromPdf(pdf);
        assertTrue(pdfText.contains(invoice.getInvoiceNumber()));
        assertTrue(pdfText.contains(invoice.getCustomerName()));
    }
}
```

### Dependencies

**Must Complete First:**
- Invoice domain model fully implemented
- InvoiceResponseDTO with all required fields
- Thymeleaf dependency already in Spring Boot classpath

**Blocks:**
- Story 5.2 (PDF template design)
- Story 5.3 (PDF download endpoint)

### Definition of Done

- [ ] iText 7 dependency successfully integrated
- [ ] InvoicePdfService interface and implementation complete
- [ ] Basic PDF template renders invoice data correctly
- [ ] Unit tests pass with >90% coverage on service logic
- [ ] Integration tests validate end-to-end PDF generation
- [ ] PDF opens successfully in Adobe Reader and Chrome PDF viewer
- [ ] Code reviewed and approved by senior developer
- [ ] Documentation added to project README

---

## Story 5.2: Invoice PDF Template Design and Implementation

**User Story:**
As a **business user**,
I want **invoices exported as PDF to have a professional, branded appearance matching the on-screen layout**,
so that **I can confidently send these PDFs to customers as official business documents**.

### Acceptance Criteria

1. **Template Layout Structure**
   - [ ] PDF template divided into clear sections: header, invoice info, line items, totals, footer
   - [ ] Layout matches the existing invoice detail page design for visual consistency
   - [ ] Responsive layout adjusts for varying line item counts without breaking
   - [ ] Page breaks handled gracefully for multi-page invoices

2. **Header Section**
   - [ ] Company name prominently displayed at top (placeholder: "InvoiceMe")
   - [ ] Company tagline/slogan included ("Professional Invoicing System")
   - [ ] Logo placeholder box reserved (150x50px) with border for visual reference
   - [ ] Company contact information area reserved (to be populated in Epic 9)
   - [ ] Header repeats on every page for multi-page invoices

3. **Invoice Details Section**
   - [ ] Invoice number displayed prominently with "Invoice #" label
   - [ ] Issue date formatted as "Issue Date: MMM DD, YYYY"
   - [ ] Due date formatted as "Due Date: MMM DD, YYYY"
   - [ ] Payment terms displayed (e.g., "Payment Terms: Net 30")
   - [ ] Invoice status shown with visual indicator (Draft/Sent/Paid)

4. **Customer Information Block**
   - [ ] "Bill To:" label clearly identifies customer section
   - [ ] Customer name displayed in bold
   - [ ] Customer email, phone, and address displayed on separate lines
   - [ ] Customer information aligned to left side of document
   - [ ] Clear visual separation from invoice details (border or spacing)

5. **Line Items Table**
   - [ ] Table headers: Description, Quantity, Unit Price, Discount, Tax Rate, Amount
   - [ ] Alternating row colors for readability (zebra striping)
   - [ ] Right-aligned numerical columns for easy reading
   - [ ] Currency formatted with $ symbol and 2 decimal places
   - [ ] Percentage values formatted correctly (e.g., "10.0%" for discount/tax)
   - [ ] Table width spans full usable page width
   - [ ] Table headers repeat on each page for multi-page invoices

6. **Totals Section**
   - [ ] Subtotal row shows sum of line items before discounts/taxes
   - [ ] Total Discount row displays aggregate discounts (if any)
   - [ ] Total Tax row shows aggregate tax amount
   - [ ] **Total Amount** displayed prominently in larger/bold font
   - [ ] Balance Due shown if invoice has payments (Total - Payments)
   - [ ] Totals section right-aligned for professional appearance
   - [ ] Currency values use consistent formatting throughout

7. **Footer Section**
   - [ ] Footer includes generation timestamp: "Generated by InvoiceMe - November 9, 2025"
   - [ ] Page numbers displayed as "Page X of Y" for multi-page invoices
   - [ ] Optional notes section for invoice-specific notes
   - [ ] Footer repeats on every page with correct page numbering

8. **Status Watermark (Draft Invoices)**
   - [ ] "DRAFT" watermark appears diagonally across page for Draft status invoices
   - [ ] Watermark is semi-transparent (30% opacity) to not obscure content
   - [ ] Watermark uses large, gray text (72pt font size)
   - [ ] Watermark positioned at 45-degree angle across center of page
   - [ ] No watermark appears for Sent or Paid status invoices

9. **Visual Design Quality**
   - [ ] Professional color scheme using neutral colors (black, gray, white with blue accents)
   - [ ] Consistent typography hierarchy (headings vs. body text)
   - [ ] Adequate whitespace prevents cluttered appearance
   - [ ] Print-friendly design (no dark backgrounds that waste ink)
   - [ ] Layout remains readable when printed in grayscale

10. **Template Data Binding**
    - [ ] All Thymeleaf expressions correctly bound to InvoiceResponseDTO properties
    - [ ] Conditional sections render only when data present (e.g., notes, discounts)
    - [ ] Null-safe expressions prevent template rendering errors
    - [ ] Date formatting uses Thymeleaf temporal utilities
    - [ ] Number formatting uses proper locale-aware formatters

### Technical Notes

**Thymeleaf Template Structure:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <title th:text="'Invoice ' + ${invoice.invoiceNumber}">Invoice</title>
    <style>
        /* Inline CSS for PDF compatibility */
        body { font-family: 'Helvetica', Arial, sans-serif; }
        .header { margin-bottom: 20px; border-bottom: 2px solid #333; }
        .line-items { width: 100%; border-collapse: collapse; }
        /* ... additional styles ... */
    </style>
</head>
<body>
    <div class="header">
        <h1 th:text="${companyName}">InvoiceMe</h1>
        <!-- Header content -->
    </div>
    <!-- Template sections -->
</body>
</html>
```

**iText Watermark Implementation:**
```java
if (invoice.getStatus() == InvoiceStatus.DRAFT) {
    PdfCanvas canvas = new PdfCanvas(page);
    canvas.setFillColor(ColorConstants.LIGHT_GRAY);
    canvas.setFontAndSize(font, 72);
    canvas.showTextAligned("DRAFT", 300, 400,
        TextAlignment.CENTER, (float) Math.toRadians(45));
}
```

**CSS Best Practices for PDF:**
- Use `page-break-inside: avoid` on table rows to prevent awkward breaks
- Set `@page` margins in CSS for consistent page layout
- Avoid `position: absolute` or `position: fixed` (limited support)
- Use `border-collapse: collapse` for tables
- Test extensively with iText's HTML renderer (limitations exist)

**Dynamic Content Handling:**
```html
<!-- Conditional rendering for discount -->
<tr th:if="${invoice.totalDiscount > 0}">
    <td colspan="5" class="text-right"><strong>Total Discount:</strong></td>
    <td class="text-right" th:text="${#numbers.formatCurrency(invoice.totalDiscount)}">$0.00</td>
</tr>
```

### Testing Requirements

**Visual Regression Testing:**
- Generate PDFs for various invoice scenarios:
  - Single-page invoice (5 line items)
  - Multi-page invoice (50 line items)
  - Invoice with discounts and taxes
  - Invoice with no discount/tax
  - Draft vs. Sent vs. Paid status
- Manually verify each PDF opens correctly in multiple viewers
- Compare PDF layout against on-screen invoice detail view

**Automated Testing:**
```java
@Test
void pdfTemplate_ShouldIncludeWatermarkForDraftInvoice() {
    InvoiceResponseDTO draftInvoice = createDraftInvoice();
    byte[] pdf = pdfService.generateInvoicePdf(draftInvoice);

    String pdfText = extractTextFromPdf(pdf);
    assertTrue(pdfText.contains("DRAFT"), "Draft watermark should be present");
}

@Test
void pdfTemplate_ShouldFormatCurrencyCorrectly() {
    InvoiceResponseDTO invoice = createInvoiceWithTotal(1234.56);
    byte[] pdf = pdfService.generateInvoicePdf(invoice);

    String pdfText = extractTextFromPdf(pdf);
    assertTrue(pdfText.contains("$1,234.56"), "Currency should be formatted");
}
```

### Dependencies

**Must Complete First:**
- Story 5.1 (PDF generation infrastructure)
- Invoice detail page UI design (for layout consistency)

**Blocks:**
- Story 5.3 (PDF download endpoint - needs template to generate PDFs)
- Story 5.4 (Company branding integration - builds on this template)

**Future Integration:**
- Epic 9 Story 9.7 (Settings integration for company logo/branding)

### Definition of Done

- [ ] PDF template includes all required sections and data fields
- [ ] Layout matches invoice detail page design
- [ ] Draft watermark displays correctly
- [ ] Multi-page invoices render with proper headers/footers
- [ ] PDF tested in Adobe Reader, Chrome, Preview.app, Edge
- [ ] Visual design approved by product owner/stakeholder
- [ ] Template performance validated (<3 seconds for 50 line items)
- [ ] Code reviewed with focus on template maintainability

---

## Story 5.3: PDF Generation API Endpoint

**User Story:**
As a **frontend developer**,
I want **a REST API endpoint that generates and streams invoice PDFs**,
so that **the UI can trigger PDF downloads for users**.

### Acceptance Criteria

1. **Endpoint Definition**
   - [ ] New endpoint created: `GET /api/invoices/{id}/pdf`
   - [ ] Endpoint placed in `InvoiceQueryController` (read operation, aligns with CQRS)
   - [ ] Path variable `{id}` correctly mapped to invoice UUID
   - [ ] Endpoint documented in OpenAPI/Swagger with example responses

2. **Authentication & Authorization**
   - [ ] Endpoint requires valid JWT token (returns 401 if unauthenticated)
   - [ ] Authorization check: user can only download PDFs for their own invoices
   - [ ] Multi-tenant consideration: tenant ID validation (if implemented)
   - [ ] Security test verifies unauthorized users cannot access PDFs

3. **Invoice Validation**
   - [ ] Endpoint validates invoice ID exists in database
   - [ ] Returns `404 Not Found` with message "Invoice not found" for invalid ID
   - [ ] Returns `400 Bad Request` for malformed UUID format
   - [ ] Handles edge case: soft-deleted invoices return 404

4. **PDF Generation**
   - [ ] Controller retrieves invoice using `GetInvoiceByIdQuery`
   - [ ] InvoiceResponseDTO passed to `InvoicePdfService.generateInvoicePdf()`
   - [ ] PDF generation errors caught and logged with invoice ID and user context
   - [ ] Returns `500 Internal Server Error` with message "PDF generation failed" on error

5. **HTTP Response Headers**
   - [ ] `Content-Type` set to `application/pdf`
   - [ ] `Content-Disposition` set to `attachment; filename="Invoice-{invoiceNumber}.pdf"`
   - [ ] `Cache-Control` set to `no-cache, no-store, must-revalidate` to prevent stale PDFs
   - [ ] `Content-Length` header set for download progress indication
   - [ ] `X-Invoice-Number` custom header includes invoice number for tracking

6. **Streaming Response**
   - [ ] PDF streamed directly to response output stream (not buffered in memory)
   - [ ] Uses Spring's `StreamingResponseBody` for efficient large file handling
   - [ ] Response flushed immediately to begin download without delay
   - [ ] Connection timeouts configured appropriately (30 seconds)

7. **Error Handling**
   - [ ] Global exception handler catches `PdfGenerationException`
   - [ ] Error responses return standardized JSON format with error code and message
   - [ ] Errors logged with correlation ID for debugging
   - [ ] User-friendly error messages returned (no stack traces exposed)

8. **Audit Logging**
   - [ ] PDF download request logged with: user ID, invoice ID, timestamp
   - [ ] Successful downloads logged at INFO level
   - [ ] Failed downloads logged at ERROR level with exception details
   - [ ] Logs include correlation ID linking request to response

9. **Integration Testing**
   - [ ] Test verifies authenticated request returns PDF successfully
   - [ ] Test validates response headers are set correctly
   - [ ] Test confirms PDF filename matches invoice number
   - [ ] Test verifies returned PDF is valid and contains invoice data
   - [ ] Test ensures 401 returned for unauthenticated requests
   - [ ] Test ensures 404 returned for non-existent invoice
   - [ ] Test validates 500 returned when PDF generation fails

### Technical Notes

**Controller Implementation:**
```java
@RestController
@RequestMapping("/api/invoices")
public class InvoiceQueryController {

    private final QueryHandler<GetInvoiceByIdQuery, InvoiceResponseDTO> getInvoiceHandler;
    private final InvoicePdfService pdfService;

    @GetMapping("/{id}/pdf")
    public ResponseEntity<StreamingResponseBody> downloadInvoicePdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Retrieve invoice
        InvoiceResponseDTO invoice = getInvoiceHandler.handle(
            new GetInvoiceByIdQuery(id, userDetails.getUsername())
        );

        // Generate PDF
        StreamingResponseBody stream = outputStream -> {
            byte[] pdfBytes = pdfService.generateInvoicePdf(invoice);
            outputStream.write(pdfBytes);
        };

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
            ContentDisposition.attachment()
                .filename("Invoice-" + invoice.getInvoiceNumber() + ".pdf")
                .build()
        );
        headers.setCacheControl(CacheControl.noStore());

        return ResponseEntity.ok()
            .headers(headers)
            .body(stream);
    }
}
```

**Error Handling:**
```java
@ExceptionHandler(PdfGenerationException.class)
public ResponseEntity<ErrorResponse> handlePdfGenerationError(
        PdfGenerationException ex,
        HttpServletRequest request) {

    log.error("PDF generation failed for request {}", request.getRequestURI(), ex);

    ErrorResponse error = new ErrorResponse(
        "PDF_GENERATION_FAILED",
        "Unable to generate PDF. Please try again later.",
        Instant.now()
    );

    return ResponseEntity.status(500).body(error);
}
```

**Performance Optimization:**
```java
// For large PDFs, stream directly without buffering
@GetMapping("/{id}/pdf")
public void downloadInvoicePdf(
        @PathVariable UUID id,
        HttpServletResponse response) throws IOException {

    InvoiceResponseDTO invoice = getInvoice(id);

    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition",
        "attachment; filename=Invoice-" + invoice.getInvoiceNumber() + ".pdf");

    // Stream directly to response
    try (OutputStream out = response.getOutputStream()) {
        pdfService.generateInvoicePdf(invoice, out);
        out.flush();
    }
}
```

### Testing Requirements

**Integration Test Example:**
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InvoicePdfDownloadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "testuser")
    void downloadPdf_WithValidInvoice_ReturnsValidPdf() throws Exception {
        // Given: Invoice exists
        UUID invoiceId = createTestInvoice();

        // When: Request PDF download
        MvcResult result = mockMvc.perform(
            get("/api/invoices/{id}/pdf", invoiceId)
                .header("Authorization", "Bearer " + validJwtToken))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().exists("Content-Disposition"))
            .andReturn();

        // Then: PDF is valid
        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(isPdfValid(pdfBytes));
    }

    @Test
    void downloadPdf_WithInvalidInvoice_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(
            get("/api/invoices/{id}/pdf", nonExistentId)
                .header("Authorization", "Bearer " + validJwtToken))
            .andExpect(status().isNotFound());
    }
}
```

### Dependencies

**Must Complete First:**
- Story 5.1 (PDF generation service)
- Story 5.2 (PDF template implementation)
- Authentication/authorization infrastructure

**Blocks:**
- Story 5.4 (Frontend PDF download integration)

### Definition of Done

- [ ] API endpoint implemented and functional
- [ ] Authentication and authorization working correctly
- [ ] Response headers set appropriately for PDF download
- [ ] Error handling covers all edge cases
- [ ] Integration tests pass with >85% coverage
- [ ] Endpoint documented in Swagger/OpenAPI
- [ ] Performance validated: <3 second response time for typical invoices
- [ ] Security review completed (no data leaks, proper authorization)
- [ ] Code reviewed and merged to main branch

---

## Story 5.4: Frontend PDF Download Integration

**User Story:**
As a **user**,
I want **to download invoice PDFs with a single click from the invoice detail page**,
so that **I can save professional invoices to my computer and send them to customers**.

### Acceptance Criteria

1. **Export PDF Button**
   - [ ] "Export PDF" button added to invoice detail page header/actions section
   - [ ] Button uses PDF icon (document icon with down arrow) from icon library
   - [ ] Button labeled clearly: "Export PDF" or "Download PDF"
   - [ ] Button positioned prominently near other invoice actions
   - [ ] Button styled consistently with other primary action buttons

2. **Button Availability**
   - [ ] Export PDF button visible for all invoice statuses (Draft, Sent, Paid)
   - [ ] Button enabled for all invoices with at least one line item
   - [ ] No disabled state needed (all invoices can be exported)
   - [ ] Button visible on desktop, tablet, and mobile views

3. **Loading State**
   - [ ] Button shows loading spinner when PDF generation in progress
   - [ ] Button text changes to "Generating PDF..." during loading
   - [ ] Button disabled during PDF generation to prevent duplicate requests
   - [ ] Loading state clears after PDF download starts or error occurs
   - [ ] Loading state includes accessible ARIA label for screen readers

4. **PDF Download Trigger**
   - [ ] Clicking button calls `GET /api/invoices/{id}/pdf` endpoint
   - [ ] Request includes JWT token in Authorization header
   - [ ] Response handled as binary blob (not parsed as JSON)
   - [ ] Browser triggers file download automatically
   - [ ] Download filename matches backend `Content-Disposition` header

5. **Download Implementation**
   - [ ] Uses `fetch` API with `blob()` to handle binary response
   - [ ] Creates temporary object URL from blob for download trigger
   - [ ] Programmatically creates and clicks `<a>` element to trigger download
   - [ ] Cleans up object URL after download to prevent memory leak
   - [ ] Handles cross-browser compatibility (Chrome, Firefox, Safari, Edge)

6. **Success Feedback**
   - [ ] Toast notification displays: "PDF downloaded successfully"
   - [ ] Toast appears immediately when download starts
   - [ ] Toast auto-dismisses after 3 seconds
   - [ ] Success toast uses green/success styling
   - [ ] No blocking modal or dialog (non-intrusive feedback)

7. **Error Handling**
   - [ ] Network errors display toast: "Failed to download PDF. Please try again."
   - [ ] 404 errors display: "Invoice not found."
   - [ ] 500 errors display: "PDF generation failed. Please contact support."
   - [ ] Error toast includes "Retry" action button
   - [ ] Error toast persists until user dismisses or retries
   - [ ] Errors logged to browser console for debugging

8. **File Handling**
   - [ ] Downloaded filename format: `Invoice-{invoiceNumber}.pdf` (e.g., "Invoice-INV-2025-0001.pdf")
   - [ ] PDF saves to user's default downloads folder
   - [ ] PDF opens correctly in system's default PDF viewer
   - [ ] PDF is searchable and copyable (not flattened image)

9. **Accessibility**
   - [ ] Button has proper ARIA label: "Download invoice PDF"
   - [ ] Button keyboard accessible (Enter/Space triggers download)
   - [ ] Loading state announced to screen readers
   - [ ] Error messages announced to screen readers via ARIA live region
   - [ ] Focus management: button regains focus after download completes

10. **Performance**
    - [ ] Download starts within 3 seconds for typical invoices
    - [ ] No unnecessary re-renders during download process
    - [ ] Network request includes timeout (30 seconds)
    - [ ] Large PDFs (multi-page) stream without blocking UI

### Technical Notes

**React Component Implementation:**
```typescript
'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Download } from 'lucide-react';
import { useToast } from '@/components/ui/use-toast';
import { downloadInvoicePdf } from '@/lib/api/invoices';

export function ExportPdfButton({ invoiceId, invoiceNumber }: Props) {
  const [isLoading, setIsLoading] = useState(false);
  const { toast } = useToast();

  const handleDownload = async () => {
    setIsLoading(true);

    try {
      // Fetch PDF as blob
      const response = await fetch(`/api/invoices/${invoiceId}/pdf`, {
        headers: {
          'Authorization': `Bearer ${getAuthToken()}`,
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      // Create blob from response
      const blob = await response.blob();

      // Create temporary download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `Invoice-${invoiceNumber}.pdf`;

      // Trigger download
      document.body.appendChild(link);
      link.click();

      // Cleanup
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      // Success feedback
      toast({
        title: 'Success',
        description: 'PDF downloaded successfully',
        variant: 'success',
      });

    } catch (error) {
      console.error('PDF download failed:', error);

      toast({
        title: 'Download Failed',
        description: 'Failed to download PDF. Please try again.',
        variant: 'destructive',
        action: <Button onClick={handleDownload}>Retry</Button>,
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Button
      onClick={handleDownload}
      disabled={isLoading}
      aria-label="Download invoice PDF"
    >
      <Download className="mr-2 h-4 w-4" />
      {isLoading ? 'Generating PDF...' : 'Export PDF'}
    </Button>
  );
}
```

**API Service Layer:**
```typescript
// lib/api/invoices.ts
export async function downloadInvoicePdf(invoiceId: string): Promise<Blob> {
  const response = await apiClient.get(`/invoices/${invoiceId}/pdf`, {
    responseType: 'blob',
    timeout: 30000, // 30 second timeout
  });

  return response.data;
}
```

**Error Boundary:**
```typescript
// Wrap component in error boundary for graceful failure
<ErrorBoundary fallback={<PdfDownloadError />}>
  <ExportPdfButton invoiceId={id} invoiceNumber={number} />
</ErrorBoundary>
```

### Testing Requirements

**Component Tests (Jest + React Testing Library):**
```typescript
describe('ExportPdfButton', () => {
  it('should trigger download when clicked', async () => {
    const mockBlob = new Blob(['fake pdf'], { type: 'application/pdf' });
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        blob: () => Promise.resolve(mockBlob),
      })
    );

    render(<ExportPdfButton invoiceId="123" invoiceNumber="INV-001" />);

    const button = screen.getByRole('button', { name: /download invoice pdf/i });
    await userEvent.click(button);

    expect(fetch).toHaveBeenCalledWith('/api/invoices/123/pdf', expect.any(Object));
  });

  it('should show loading state during download', async () => {
    render(<ExportPdfButton invoiceId="123" invoiceNumber="INV-001" />);

    const button = screen.getByRole('button');
    await userEvent.click(button);

    expect(button).toHaveTextContent('Generating PDF...');
    expect(button).toBeDisabled();
  });

  it('should display error toast on failure', async () => {
    global.fetch = jest.fn(() => Promise.reject(new Error('Network error')));

    render(<ExportPdfButton invoiceId="123" invoiceNumber="INV-001" />);

    await userEvent.click(screen.getByRole('button'));

    expect(await screen.findByText(/failed to download pdf/i)).toBeInTheDocument();
  });
});
```

**E2E Tests (Playwright):**
```typescript
test('user can download invoice PDF', async ({ page }) => {
  await page.goto('/invoices/123');

  // Setup download handler
  const downloadPromise = page.waitForEvent('download');

  // Click export button
  await page.getByRole('button', { name: /export pdf/i }).click();

  // Wait for download
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe('Invoice-INV-2025-0001.pdf');

  // Verify file is valid PDF
  const path = await download.path();
  const buffer = await fs.readFile(path);
  expect(buffer.toString('utf8', 0, 4)).toBe('%PDF');
});
```

### Dependencies

**Must Complete First:**
- Story 5.3 (PDF generation API endpoint)
- Invoice detail page UI component
- Toast notification system (Shadcn/ui)

**Blocks:**
- None (this completes basic PDF export functionality)

**Related:**
- Story 5.5 (Bulk PDF export) - extends this functionality

### Definition of Done

- [ ] Export PDF button added to invoice detail page
- [ ] Button triggers PDF download successfully
- [ ] Loading and error states work correctly
- [ ] Success/error toast notifications display appropriately
- [ ] Downloaded PDFs open in PDF viewers (Adobe, Chrome, Preview)
- [ ] Component tests pass with >90% coverage
- [ ] E2E test validates full download flow
- [ ] Accessibility audit passes (keyboard, screen reader)
- [ ] Cross-browser testing completed (Chrome, Firefox, Safari, Edge)
- [ ] Mobile responsive testing completed (download works on mobile)
- [ ] Code reviewed and approved
- [ ] Feature demoed to product owner

---

## Story 5.5: Bulk PDF Export Functionality

**User Story:**
As a **user**,
I want **to export multiple invoices as PDFs in a single operation**,
so that **I can efficiently prepare and send invoices to multiple customers at once**.

### Acceptance Criteria

1. **Bulk Selection Integration**
   - [ ] "Export PDFs" action added to bulk actions toolbar (Epic 8 integration point)
   - [ ] Action enabled when 1 or more invoices selected
   - [ ] Action available for all invoice statuses (Draft, Sent, Paid)
   - [ ] Maximum selection limit documented (e.g., 50 invoices per batch)

2. **Confirmation Dialog**
   - [ ] Clicking "Export PDFs" opens confirmation modal
   - [ ] Modal shows count: "Export X invoices as PDF?"
   - [ ] Modal lists selected invoice numbers (first 5, then "and X more...")
   - [ ] Warning message: "This will download X PDF files to your computer"
   - [ ] Modal has "Cancel" and "Export PDFs" buttons

3. **Bulk Download Options**
   - [ ] **Option A (MVP):** Individual file downloads - Each PDF downloads separately
   - [ ] **Option B (Future):** ZIP archive - All PDFs packaged in single ZIP file
   - [ ] Implementation uses Option A for simplicity (Option B deferred)
   - [ ] Modal clearly explains download behavior based on selected option

4. **Progress Indicator**
   - [ ] Progress modal displays after confirmation
   - [ ] Progress bar shows: "Exporting 3 of 10 invoices..."
   - [ ] Percentage completion updates in real-time
   - [ ] User can cancel operation mid-process (optional for MVP)
   - [ ] Progress modal prevents interaction with underlying page (modal backdrop)

5. **Sequential Download**
   - [ ] PDFs downloaded one at a time to avoid browser blocking
   - [ ] Delay between downloads (500ms) to prevent browser popup blocker
   - [ ] Each successful download increments progress counter
   - [ ] Failed downloads logged but don't stop remaining downloads
   - [ ] All downloads complete before showing final results

6. **Error Handling**
   - [ ] Track success and failure counts for each invoice
   - [ ] If all downloads succeed: Success toast "All X PDFs downloaded successfully"
   - [ ] If some fail: Warning toast "X of Y PDFs downloaded. Z failed."
   - [ ] Failed invoice details shown in expandable error list
   - [ ] Retry option for failed invoices only

7. **Results Summary**
   - [ ] Results modal displays after all downloads attempt
   - [ ] Summary shows: Total attempted, Successful, Failed
   - [ ] Failed invoices list includes: invoice number, customer name, error reason
   - [ ] "Download Failed PDFs Again" button retries only failures
   - [ ] "Close" button dismisses results and clears selection

8. **Performance Optimization**
   - [ ] Maximum batch size enforced (50 invoices recommended)
   - [ ] Warning shown if user selects >20 invoices
   - [ ] PDF generation requests made sequentially (not parallel) to avoid server overload
   - [ ] Timeout per PDF set to 30 seconds
   - [ ] User can navigate away during download (background processing optional)

9. **File Organization**
   - [ ] Each PDF filename follows pattern: `Invoice-{invoiceNumber}.pdf`
   - [ ] All PDFs save to user's downloads folder
   - [ ] Files downloaded in order of selection or invoice number
   - [ ] Duplicate filenames handled by browser (appends number)

10. **Accessibility & UX**
    - [ ] Progress announced to screen readers via ARIA live region
    - [ ] Keyboard users can tab through confirmation/results modals
    - [ ] Escape key closes modals (except during active download)
    - [ ] Focus trapped within modal during download
    - [ ] Clear visual feedback for current download status

### Technical Notes

**Frontend Implementation Strategy:**

**Sequential Download Function:**
```typescript
async function downloadInvoicesPdf(invoiceIds: string[]): Promise<DownloadResult> {
  const results: DownloadResult = {
    total: invoiceIds.length,
    successful: [],
    failed: [],
  };

  for (let i = 0; i < invoiceIds.length; i++) {
    const invoiceId = invoiceIds[i];

    try {
      // Update progress
      onProgress?.({
        current: i + 1,
        total: invoiceIds.length,
        invoiceId,
      });

      // Download PDF
      await downloadSinglePdf(invoiceId);
      results.successful.push(invoiceId);

      // Delay to prevent browser blocking
      if (i < invoiceIds.length - 1) {
        await new Promise(resolve => setTimeout(resolve, 500));
      }
    } catch (error) {
      console.error(`Failed to download invoice ${invoiceId}:`, error);
      results.failed.push({
        invoiceId,
        error: error.message,
      });
    }
  }

  return results;
}
```

**Progress Modal Component:**
```typescript
function BulkPdfProgressModal({ invoiceIds, onComplete }: Props) {
  const [progress, setProgress] = useState(0);
  const [currentInvoice, setCurrentInvoice] = useState('');

  useEffect(() => {
    const download = async () => {
      const results = await downloadInvoicesPdf(invoiceIds, {
        onProgress: ({ current, total, invoiceId }) => {
          setProgress((current / total) * 100);
          setCurrentInvoice(invoiceId);
        },
      });

      onComplete(results);
    };

    download();
  }, [invoiceIds]);

  return (
    <Dialog open>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Exporting Invoices</DialogTitle>
        </DialogHeader>

        <Progress value={progress} className="w-full" />

        <p className="text-sm text-muted-foreground">
          Downloading invoice {currentInvoice}... ({Math.round(progress)}%)
        </p>
      </DialogContent>
    </Dialog>
  );
}
```

**Alternative: ZIP Archive (Future Enhancement):**
```typescript
// Backend endpoint: POST /api/invoices/export/bulk-pdf
// Request body: { invoiceIds: string[] }
// Response: ZIP file with all PDFs

async function downloadBulkPdfAsZip(invoiceIds: string[]) {
  const response = await apiClient.post(
    '/invoices/export/bulk-pdf',
    { invoiceIds },
    { responseType: 'blob', timeout: 120000 }
  );

  const blob = response.data;
  downloadBlob(blob, `Invoices-${new Date().toISOString()}.zip`);
}
```

**Backend Consideration (If ZIP Approach):**
```java
@PostMapping("/export/bulk-pdf")
public ResponseEntity<StreamingResponseBody> bulkExportPdf(
        @RequestBody BulkExportRequest request) {

    // Validate request
    if (request.getInvoiceIds().size() > 50) {
        throw new ValidationException("Maximum 50 invoices per export");
    }

    StreamingResponseBody stream = outputStream -> {
        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
            for (UUID invoiceId : request.getInvoiceIds()) {
                InvoiceResponseDTO invoice = getInvoice(invoiceId);
                byte[] pdf = pdfService.generateInvoicePdf(invoice);

                ZipEntry entry = new ZipEntry("Invoice-" + invoice.getInvoiceNumber() + ".pdf");
                zipOut.putNextEntry(entry);
                zipOut.write(pdf);
                zipOut.closeEntry();
            }
        }
    };

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/zip"));
    headers.setContentDisposition(
        ContentDisposition.attachment()
            .filename("Invoices-Export-" + LocalDate.now() + ".zip")
            .build()
    );

    return ResponseEntity.ok().headers(headers).body(stream);
}
```

### Testing Requirements

**Component Tests:**
```typescript
describe('BulkPdfExport', () => {
  it('should download all selected invoices', async () => {
    const invoiceIds = ['id1', 'id2', 'id3'];

    render(<BulkPdfExport invoiceIds={invoiceIds} />);

    await userEvent.click(screen.getByRole('button', { name: /export pdfs/i }));
    await userEvent.click(screen.getByRole('button', { name: /confirm/i }));

    await waitFor(() => {
      expect(downloadSinglePdf).toHaveBeenCalledTimes(3);
    });

    expect(screen.getByText(/all 3 pdfs downloaded/i)).toBeInTheDocument();
  });

  it('should handle partial failures gracefully', async () => {
    downloadSinglePdf
      .mockResolvedValueOnce(undefined)  // Success
      .mockRejectedValueOnce(new Error('Network error'))  // Fail
      .mockResolvedValueOnce(undefined);  // Success

    render(<BulkPdfExport invoiceIds={['id1', 'id2', 'id3']} />);

    await performBulkExport();

    expect(screen.getByText(/2 of 3 pdfs downloaded/i)).toBeInTheDocument();
    expect(screen.getByText(/1 failed/i)).toBeInTheDocument();
  });
});
```

**E2E Tests:**
```typescript
test('bulk PDF export downloads multiple files', async ({ page }) => {
  await page.goto('/invoices');

  // Select multiple invoices
  await page.getByRole('checkbox', { name: /select invoice inv-001/i }).check();
  await page.getByRole('checkbox', { name: /select invoice inv-002/i }).check();

  // Track downloads
  const downloads: Download[] = [];
  page.on('download', download => downloads.push(download));

  // Trigger bulk export
  await page.getByRole('button', { name: /export pdfs/i }).click();
  await page.getByRole('button', { name: /confirm/i }).click();

  // Wait for downloads to complete
  await page.waitForSelector('text=All 2 PDFs downloaded successfully');

  expect(downloads).toHaveLength(2);
  expect(downloads[0].suggestedFilename()).toMatch(/^Invoice-INV-\d+\.pdf$/);
});
```

### Dependencies

**Must Complete First:**
- Story 5.4 (Single PDF download - reuse logic)
- Epic 8 Story 8.4 (Bulk selection UI - provides invoice IDs)

**Blocks:**
- None (feature is enhancement, not blocker)

**Related:**
- Epic 8 (Bulk Operations) - provides selection infrastructure

### Definition of Done

- [ ] Bulk PDF export action integrated with bulk selection UI
- [ ] Confirmation modal implemented
- [ ] Progress indicator shows real-time download status
- [ ] Sequential download logic works correctly
- [ ] Error handling displays partial failure results
- [ ] Results summary modal implemented
- [ ] Component tests pass with >85% coverage
- [ ] E2E test validates multi-file download
- [ ] Performance tested with maximum batch size (50 invoices)
- [ ] User experience validated (no browser popup blocking)
- [ ] Documentation updated with bulk export feature
- [ ] Code reviewed and approved
- [ ] Feature demoed to stakeholders

---

## Epic 5 Summary

### Story Priority Order

**Recommended Implementation Sequence:**
1. **Story 5.1** (Infrastructure) - Foundation for all PDF work
2. **Story 5.2** (Template) - Visual design and layout
3. **Story 5.3** (API Endpoint) - Backend integration
4. **Story 5.4** (Frontend UI) - User-facing feature ✅ **MVP Complete**
5. **Story 5.5** (Bulk Export) - Enhancement (optional for MVP)

### Epic Completion Criteria

**MVP Complete When:**
- [ ] Stories 5.1-5.4 fully implemented and tested
- [ ] Users can download individual invoice PDFs from invoice detail page
- [ ] PDFs match on-screen layout and include all invoice data
- [ ] Performance target met: <3 seconds for typical invoices
- [ ] Cross-browser compatibility validated
- [ ] Integration with Epic 9 (Settings) planned and documented

**Full Epic Complete When:**
- [ ] Story 5.5 (Bulk export) implemented
- [ ] All acceptance criteria met across all stories
- [ ] Epic documentation updated
- [ ] Production deployment checklist completed

### Future Enhancements (Post-Epic 9)

**Settings Integration (Epic 9 Story 9.7):**
- Replace placeholder company name with settings data
- Add company logo to PDF header
- Include custom footer text from settings
- Support customizable branding colors

**Advanced Features (Future):**
- Email PDF as attachment (Epic 7 integration)
- PDF preview before download
- Custom PDF templates per customer
- Batch PDF generation with ZIP download
- PDF archival/storage in cloud (S3/Azure Blob)
- PDF digital signatures for authenticity

### Technical Debt & Maintenance Notes

**Known Limitations:**
- iText AGPL license requires commercial license for proprietary software
- HTML-to-PDF conversion has CSS limitations (document in README)
- Large invoices (100+ line items) may exceed 3-second target
- Base64 logo storage in template is temporary solution

**Monitoring & Operations:**
- Add metrics for PDF generation time
- Monitor PDF generation failure rate
- Track PDF download counts for analytics
- Log performance degradation for optimization

### Related Documentation

- **PRD Reference:** Epic 5 (Lines 198-327 in `/docs/missing_features_prd.md`)
- **Architecture:** PDF Generation section in `/docs/architecture.md`
- **API Documentation:** OpenAPI spec for `/api/invoices/{id}/pdf`
- **Testing Guide:** `/docs/testing/pdf-export-tests.md` (to be created)

---

**Document Version:** 1.0
**Last Updated:** November 9, 2025
**Author:** Story Writer AI
**Reviewers:** Product Owner, Tech Lead
**Status:** Ready for Sprint Planning
