package com.invoiceme.builders;

import com.invoiceme.invoice.commands.dto.LineItemDTO;
import com.invoiceme.invoice.domain.LineItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for LineItem test data
 */
public class LineItemTestBuilder {

    private String description = "Test Line Item";
    private int quantity = 1;
    private BigDecimal unitPrice = new BigDecimal("100.00");
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private BigDecimal taxRate = BigDecimal.ZERO;

    private LineItemTestBuilder() {
    }

    public static LineItemTestBuilder create() {
        return new LineItemTestBuilder();
    }

    public LineItemTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public LineItemTestBuilder withQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    public LineItemTestBuilder withUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public LineItemTestBuilder withUnitPrice(String unitPrice) {
        this.unitPrice = new BigDecimal(unitPrice);
        return this;
    }

    public LineItemTestBuilder withDiscount(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
        return this;
    }

    public LineItemTestBuilder withDiscount(String discountPercent) {
        this.discountPercent = new BigDecimal(discountPercent);
        return this;
    }

    public LineItemTestBuilder withTax(BigDecimal taxRate) {
        this.taxRate = taxRate;
        return this;
    }

    public LineItemTestBuilder withTax(String taxRate) {
        this.taxRate = new BigDecimal(taxRate);
        return this;
    }

    public LineItemTestBuilder withDefaults() {
        this.description = "Test Line Item";
        this.quantity = 1;
        this.unitPrice = new BigDecimal("100.00");
        this.discountPercent = BigDecimal.ZERO;
        this.taxRate = BigDecimal.ZERO;
        return this;
    }

    /**
     * Build LineItem value object
     */
    public LineItem build() {
        return new LineItem(null, description, quantity, unitPrice, discountPercent, taxRate);
    }

    /**
     * Build LineItemDTO for API requests
     */
    public LineItemDTO buildDTO() {
        return new LineItemDTO(description, quantity, unitPrice, discountPercent, taxRate);
    }

    /**
     * Build multiple LineItem instances
     */
    public List<LineItem> buildList(int count) {
        List<LineItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(
                LineItemTestBuilder.create()
                    .withDescription("Line Item " + (i + 1))
                    .withQuantity(i + 1)
                    .withUnitPrice(new BigDecimal("50.00"))
                    .build()
            );
        }
        return items;
    }

    /**
     * Build multiple LineItemDTO instances
     */
    public List<LineItemDTO> buildDTOList(int count) {
        List<LineItemDTO> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(
                LineItemTestBuilder.create()
                    .withDescription("Line Item " + (i + 1))
                    .withQuantity(i + 1)
                    .withUnitPrice(new BigDecimal("50.00"))
                    .buildDTO()
            );
        }
        return items;
    }
}
