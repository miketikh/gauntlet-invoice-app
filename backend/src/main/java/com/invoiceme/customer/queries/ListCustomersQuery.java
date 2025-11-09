package com.invoiceme.customer.queries;

/**
 * Query to list customers with pagination, sorting, and filtering
 */
public record ListCustomersQuery(
    int page,
    int size,
    String sortBy,
    String sortDirection,
    String search
) {
    public ListCustomersQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
        // Set defaults for null values
        sortBy = sortBy != null ? sortBy : "name";
        sortDirection = sortDirection != null ? sortDirection : "asc";
        search = search != null ? search.trim() : "";
    }

    // Constructor with defaults
    public ListCustomersQuery(int page, int size) {
        this(page, size, "name", "asc", "");
    }
}
