package com.invoiceme.customer.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.queries.dto.CustomerListItemDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler for ListCustomersQuery
 * Retrieves paginated list of customers with filtering and sorting
 * Uses caching and batch queries to optimize performance
 */
@Component
public class ListCustomersQueryHandler {

    private final CustomerQueryRepository customerQueryRepository;

    public ListCustomersQueryHandler(CustomerQueryRepository customerQueryRepository) {
        this.customerQueryRepository = customerQueryRepository;
    }

    /**
     * Handles the ListCustomersQuery
     * @param query Query containing pagination, sorting, and filtering parameters
     * @return Page of CustomerListItemDTO
     */
    @Cacheable(value = "customerList", key = "#query.toString()")
    public Page<CustomerListItemDTO> handle(ListCustomersQuery query) {
        Pageable pageable = createPageable(query);

        Page<Customer> customerPage;
        if (query.search() != null && !query.search().isEmpty()) {
            customerPage = customerQueryRepository.searchByNameOrEmail(query.search(), pageable);
        } else {
            customerPage = customerQueryRepository.findAllNotDeleted(pageable);
        }

        // Batch fetch invoice counts and balances to avoid N+1 queries
        List<UUID> customerIds = customerPage.getContent().stream()
            .map(Customer::getId)
            .collect(Collectors.toList());

        Map<UUID, Integer> invoiceCounts = new HashMap<>();
        Map<UUID, BigDecimal> outstandingBalances = new HashMap<>();

        // Batch queries (could be further optimized with native queries if needed)
        for (UUID customerId : customerIds) {
            invoiceCounts.put(customerId, customerQueryRepository.countInvoicesByCustomerId(customerId));
            outstandingBalances.put(customerId, customerQueryRepository.calculateOutstandingBalance(customerId));
        }

        return customerPage.map(customer -> mapToListItemDTO(customer, invoiceCounts, outstandingBalances));
    }

    /**
     * Creates Pageable object from query parameters
     */
    private Pageable createPageable(ListCustomersQuery query) {
        Sort sort = createSort(query.sortBy(), query.sortDirection());
        return PageRequest.of(query.page(), query.size(), sort);
    }

    /**
     * Creates Sort object from sort field and direction
     */
    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        // Validate and map sort field
        String sortField = switch (sortBy.toLowerCase()) {
            case "email" -> "email";
            case "createdat" -> "createdAt";
            case "name" -> "name";
            default -> "name"; // Default to name if invalid field
        };

        return Sort.by(direction, sortField);
    }

    /**
     * Maps Customer entity to CustomerListItemDTO using pre-fetched data
     */
    private CustomerListItemDTO mapToListItemDTO(Customer customer,
                                                  Map<UUID, Integer> invoiceCounts,
                                                  Map<UUID, BigDecimal> outstandingBalances) {
        Integer totalInvoices = invoiceCounts.getOrDefault(customer.getId(), 0);
        BigDecimal outstandingBalance = outstandingBalances.getOrDefault(customer.getId(), BigDecimal.ZERO);

        return new CustomerListItemDTO(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getCreatedAt(),
            totalInvoices,
            outstandingBalance
        );
    }
}
