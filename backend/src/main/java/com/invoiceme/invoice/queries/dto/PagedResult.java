package com.invoiceme.invoice.queries.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic pagination wrapper for query results
 * Provides page metadata along with content
 */
public record PagedResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    /**
     * Factory method to create PagedResult from Spring Data Page
     * @param springPage Spring Data Page object
     * @return PagedResult with same data and metadata
     */
    public static <T> PagedResult<T> from(Page<T> springPage) {
        return new PagedResult<>(
            springPage.getContent(),
            springPage.getNumber(),
            springPage.getSize(),
            springPage.getTotalElements(),
            springPage.getTotalPages()
        );
    }
}
