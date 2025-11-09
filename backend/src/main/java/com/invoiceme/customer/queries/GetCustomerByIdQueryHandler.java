package com.invoiceme.customer.queries;

import com.invoiceme.customer.domain.Customer;
import com.invoiceme.customer.domain.CustomerNotFoundException;
import com.invoiceme.customer.queries.dto.CustomerResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Handler for GetCustomerByIdQuery
 * Retrieves a single customer by ID with computed fields
 */
@Component
public class GetCustomerByIdQueryHandler {

    private final CustomerQueryRepository customerQueryRepository;

    public GetCustomerByIdQueryHandler(CustomerQueryRepository customerQueryRepository) {
        this.customerQueryRepository = customerQueryRepository;
    }

    /**
     * Handles the GetCustomerByIdQuery
     * @param query Query containing customer ID
     * @return CustomerResponseDTO with all customer data and computed fields
     * @throws CustomerNotFoundException if customer not found or deleted
     */
    public CustomerResponseDTO handle(GetCustomerByIdQuery query) {
        Customer customer = customerQueryRepository.findById(query.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(query.customerId()));

        // Compute derived fields
        Integer totalInvoices = customerQueryRepository.countInvoicesByCustomerId(customer.getId());
        BigDecimal outstandingBalance = customerQueryRepository.calculateOutstandingBalance(customer.getId());

        return mapToDTO(customer, totalInvoices, outstandingBalance);
    }

    /**
     * Maps Customer entity to CustomerResponseDTO
     */
    private CustomerResponseDTO mapToDTO(Customer customer, Integer totalInvoices, BigDecimal outstandingBalance) {
        return new CustomerResponseDTO(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getAddress(),
            customer.getCreatedAt(),
            customer.getUpdatedAt(),
            totalInvoices,
            outstandingBalance
        );
    }
}
