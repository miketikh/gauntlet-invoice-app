package com.invoiceme.customer.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.queries.dto.CustomerListItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Handler for ListCustomersQuery
 * Retrieves paginated list of customers with filtering and sorting
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
    public Page<CustomerListItemDTO> handle(ListCustomersQuery query) {
        Pageable pageable = createPageable(query);

        Page<Customer> customerPage;
        if (query.search() != null && !query.search().isEmpty()) {
            customerPage = customerQueryRepository.searchByNameOrEmail(query.search(), pageable);
        } else {
            customerPage = customerQueryRepository.findAllNotDeleted(pageable);
        }

        return customerPage.map(this::mapToListItemDTO);
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
     * Maps Customer entity to CustomerListItemDTO
     */
    private CustomerListItemDTO mapToListItemDTO(Customer customer) {
        // Compute derived fields for each customer
        Integer totalInvoices = customerQueryRepository.countInvoicesByCustomerId(customer.getId());
        BigDecimal outstandingBalance = customerQueryRepository.calculateOutstandingBalance(customer.getId());

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
