package com.invoiceme.invoice.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerRepository;
import com.invoiceme.invoice.commands.dto.InvoiceMapper;
import com.invoiceme.invoice.domain.Invoice;
import com.invoiceme.invoice.infrastructure.JpaInvoiceRepository;
import com.invoiceme.invoice.queries.dto.InvoiceListItemDTO;
import com.invoiceme.invoice.queries.dto.PagedResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler for ListInvoicesQuery
 * Retrieves paginated list of invoices with filtering and sorting
 */
@Service
public class ListInvoicesQueryHandler {

    private final JpaInvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public ListInvoicesQueryHandler(JpaInvoiceRepository invoiceRepository,
                                    CustomerRepository customerRepository) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Handles the query to list invoices
     * @param query The query with filters and pagination
     * @return PagedResult of InvoiceListItemDTO
     */
    @Transactional(readOnly = true)
    public PagedResult<InvoiceListItemDTO> handle(ListInvoicesQuery query) {
        // Create pageable with sorting
        Sort sort = Sort.by(
            query.sortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
            query.sortBy()
        );
        Pageable pageable = PageRequest.of(query.page(), query.size(), sort);

        // Query invoices with filters
        Page<Invoice> invoicePage = invoiceRepository.findWithFilters(
            query.customerId(),
            query.status(),
            query.startDate(),
            query.endDate(),
            pageable
        );

        // Get unique customer IDs
        List<UUID> customerIds = invoicePage.getContent().stream()
            .map(Invoice::getCustomerId)
            .distinct()
            .collect(Collectors.toList());

        // Fetch all customers at once (avoid N+1)
        Map<UUID, String> customerNames = new HashMap<>();
        if (!customerIds.isEmpty()) {
            for (UUID customerId : customerIds) {
                customerRepository.findById(customerId).ifPresent(customer ->
                    customerNames.put(customerId, customer.getName())
                );
            }
        }

        // Convert to DTOs
        List<InvoiceListItemDTO> items = invoicePage.getContent().stream()
            .map(invoice -> {
                String customerName = customerNames.getOrDefault(
                    invoice.getCustomerId(),
                    "Unknown Customer"
                );
                return InvoiceMapper.toInvoiceListItemDTO(invoice, customerName);
            })
            .collect(Collectors.toList());

        // Return paged result
        return new PagedResult<>(
            items,
            invoicePage.getNumber(),
            invoicePage.getSize(),
            invoicePage.getTotalElements(),
            invoicePage.getTotalPages()
        );
    }
}
