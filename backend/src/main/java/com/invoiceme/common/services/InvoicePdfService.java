package com.invoiceme.common.services;

import com.invoiceme.invoice.commands.dto.InvoiceResponseDTO;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Service interface for generating PDF documents from invoices.
 *
 * <p>This service provides methods to convert InvoiceResponseDTO objects into
 * PDF format, supporting both byte array and streaming output modes for
 * flexible integration with different delivery mechanisms.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Autowired
 * private InvoicePdfService pdfService;
 *
 * public void exportInvoice(InvoiceResponseDTO invoice) {
 *     try {
 *         byte[] pdfBytes = pdfService.generateInvoicePdf(invoice);
 *         // Save or send the PDF bytes
 *     } catch (PdfGenerationException e) {
 *         // Handle generation error
 *         log.error("Failed to generate PDF for invoice {}", invoice.invoiceNumber(), e);
 *     }
 * }
 * }</pre>
 *
 * <h2>Error Handling:</h2>
 * <p>All methods throw {@link PdfGenerationException} when PDF generation fails.
 * This exception wraps underlying iText or template rendering errors and includes
 * contextual information about the invoice being processed.</p>
 *
 * <h2>Performance Considerations:</h2>
 * <ul>
 *   <li>PDF generation should complete within 3 seconds for typical invoices</li>
 *   <li>For large documents, prefer streaming methods to reduce memory usage</li>
 *   <li>Template and font caching is handled internally for optimal performance</li>
 * </ul>
 *
 * @see com.invoiceme.invoice.commands.dto.InvoiceResponseDTO
 * @see PdfGenerationException
 */
public interface InvoicePdfService {

    /**
     * Generates a PDF document for the given invoice and returns it as a byte array.
     *
     * <p>This method is suitable for scenarios where the PDF needs to be stored,
     * cached, or processed in memory before delivery. For direct streaming to
     * HTTP responses or files, consider using {@link #generateInvoicePdf(InvoiceResponseDTO, OutputStream)}.</p>
     *
     * @param invoice the invoice data to render as PDF, must not be null
     * @return byte array containing the complete PDF document
     * @throws PdfGenerationException if PDF generation fails due to template errors,
     *         rendering issues, or I/O problems
     * @throws IllegalArgumentException if invoice is null
     */
    byte[] generateInvoicePdf(InvoiceResponseDTO invoice);

    /**
     * Generates a PDF document for the given invoice and returns it as an InputStream.
     *
     * <p>This method provides a streaming alternative to {@link #generateInvoicePdf(InvoiceResponseDTO)}
     * for scenarios where consuming the PDF as a stream is more efficient than
     * loading the entire document into memory.</p>
     *
     * <p><strong>Note:</strong> The caller is responsible for closing the returned InputStream
     * to free system resources.</p>
     *
     * @param invoice the invoice data to render as PDF, must not be null
     * @return InputStream containing the PDF document data
     * @throws PdfGenerationException if PDF generation fails
     * @throws IllegalArgumentException if invoice is null
     */
    InputStream generateInvoicePdfStream(InvoiceResponseDTO invoice);

    /**
     * Generates a PDF document for the given invoice and writes it directly to the
     * provided OutputStream.
     *
     * <p>This method is optimized for scenarios where the PDF should be written
     * directly to an HTTP response or file, avoiding intermediate buffering in memory.
     * This is the most memory-efficient option for large invoices.</p>
     *
     * <p><strong>Note:</strong> This method does NOT close the output stream. The caller
     * is responsible for managing the stream lifecycle.</p>
     *
     * @param invoice the invoice data to render as PDF, must not be null
     * @param outputStream the stream to write the PDF data to, must not be null
     * @throws PdfGenerationException if PDF generation fails
     * @throws IllegalArgumentException if invoice or outputStream is null
     */
    void generateInvoicePdf(InvoiceResponseDTO invoice, OutputStream outputStream);
}
