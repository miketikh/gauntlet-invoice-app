package com.invoiceme.common.exceptions;

import java.util.Map;
import java.util.UUID;

/**
 * Exception thrown when PDF generation fails.
 *
 * <p>This exception wraps underlying errors from the PDF generation process,
 * including template rendering failures, iText processing errors, and I/O issues.
 * It extends {@link DomainException} to maintain consistency with the application's
 * exception hierarchy.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * try {
 *     // PDF generation code
 *     htmlConverter.convertToPdf(html, outputStream);
 * } catch (IOException e) {
 *     throw new PdfGenerationException(
 *         "Failed to generate PDF for invoice " + invoiceNumber,
 *         invoiceId,
 *         e
 *     );
 * }
 * }</pre>
 *
 * <p>The exception includes contextual information such as invoice ID and invoice number
 * to aid in debugging and logging.</p>
 *
 * @see DomainException
 * @see com.invoiceme.common.services.InvoicePdfService
 */
public class PdfGenerationException extends DomainException {

    private static final String ERROR_CODE = "PDF_GENERATION_FAILED";

    /**
     * Creates a new PdfGenerationException with the specified message.
     *
     * @param message the error message describing why PDF generation failed
     */
    public PdfGenerationException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Creates a new PdfGenerationException with the specified message and cause.
     *
     * @param message the error message describing why PDF generation failed
     * @param cause the underlying exception that caused the PDF generation failure
     */
    public PdfGenerationException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    /**
     * Creates a new PdfGenerationException with the specified message and invoice ID.
     *
     * @param message the error message describing why PDF generation failed
     * @param invoiceId the ID of the invoice that failed to generate
     */
    public PdfGenerationException(String message, UUID invoiceId) {
        super(message, ERROR_CODE, Map.of("invoiceId", invoiceId.toString()));
    }

    /**
     * Creates a new PdfGenerationException with the specified message, invoice ID, and cause.
     *
     * @param message the error message describing why PDF generation failed
     * @param invoiceId the ID of the invoice that failed to generate
     * @param cause the underlying exception that caused the PDF generation failure
     */
    public PdfGenerationException(String message, UUID invoiceId, Throwable cause) {
        super(message, ERROR_CODE, Map.of("invoiceId", invoiceId.toString()), cause);
    }

    /**
     * Creates a new PdfGenerationException with detailed context information.
     *
     * @param message the error message describing why PDF generation failed
     * @param invoiceId the ID of the invoice that failed to generate
     * @param invoiceNumber the invoice number for easier identification
     * @param cause the underlying exception that caused the PDF generation failure
     */
    public PdfGenerationException(String message, UUID invoiceId, String invoiceNumber, Throwable cause) {
        super(message, ERROR_CODE, Map.of(
            "invoiceId", invoiceId.toString(),
            "invoiceNumber", invoiceNumber
        ), cause);
    }

    /**
     * Creates a new PdfGenerationException with custom details.
     *
     * @param message the error message describing why PDF generation failed
     * @param details additional context information about the failure
     * @param cause the underlying exception that caused the PDF generation failure
     */
    public PdfGenerationException(String message, Map<String, Object> details, Throwable cause) {
        super(message, ERROR_CODE, details, cause);
    }
}
