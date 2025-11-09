package com.invoiceme.invoice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for LineItem value object
 */
@DisplayName("LineItem Value Object Tests")
class LineItemTest {

    @Test
    @DisplayName("Should create valid line item with all fields")
    void shouldCreateValidLineItem() {
        // Arrange & Act
        LineItem lineItem = new LineItem(
            "test-id",
            "Software License",
            5,
            new BigDecimal("100.00"),
            new BigDecimal("0.10"),  // 10% discount
            new BigDecimal("0.08")   // 8% tax
        );

        // Assert
        assertThat(lineItem).isNotNull();
        assertThat(lineItem.id()).isEqualTo("test-id");
        assertThat(lineItem.description()).isEqualTo("Software License");
        assertThat(lineItem.quantity()).isEqualTo(5);
        assertThat(lineItem.unitPrice()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(lineItem.discountPercent()).isEqualByComparingTo(new BigDecimal("0.10"));
        assertThat(lineItem.taxRate()).isEqualByComparingTo(new BigDecimal("0.08"));
    }

    @Test
    @DisplayName("Should auto-generate ID when not provided")
    void shouldAutoGenerateId() {
        // Arrange & Act
        LineItem lineItem = new LineItem(
            null,
            "Test Item",
            1,
            new BigDecimal("50.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );

        // Assert
        assertThat(lineItem.id()).isNotNull();
        assertThat(lineItem.id()).isNotBlank();
    }

    @Test
    @DisplayName("Should calculate subtotal correctly")
    void shouldCalculateSubtotal() {
        // Arrange
        LineItem lineItem = new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );

        // Act
        BigDecimal subtotal = lineItem.subtotal();

        // Assert
        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("Should calculate discount amount correctly")
    void shouldCalculateDiscountAmount() {
        // Arrange
        LineItem lineItem = new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            new BigDecimal("0.10"),  // 10% discount
            BigDecimal.ZERO
        );

        // Act
        BigDecimal discountAmount = lineItem.discountAmount();

        // Assert
        assertThat(discountAmount).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Should calculate taxable amount correctly")
    void shouldCalculateTaxableAmount() {
        // Arrange
        LineItem lineItem = new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            new BigDecimal("0.10"),  // 10% discount
            new BigDecimal("0.08")   // 8% tax
        );

        // Act
        BigDecimal taxableAmount = lineItem.taxableAmount();

        // Assert
        // Subtotal: 500, Discount: 50, Taxable: 450
        assertThat(taxableAmount).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    @DisplayName("Should calculate tax amount correctly")
    void shouldCalculateTaxAmount() {
        // Arrange
        LineItem lineItem = new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            new BigDecimal("0.10"),  // 10% discount
            new BigDecimal("0.08")   // 8% tax
        );

        // Act
        BigDecimal taxAmount = lineItem.taxAmount();

        // Assert
        // Taxable: 450, Tax: 450 * 0.08 = 36.00
        assertThat(taxAmount).isEqualByComparingTo(new BigDecimal("36.00"));
    }

    @Test
    @DisplayName("Should calculate total correctly")
    void shouldCalculateTotal() {
        // Arrange
        LineItem lineItem = new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            new BigDecimal("0.10"),  // 10% discount
            new BigDecimal("0.08")   // 8% tax
        );

        // Act
        BigDecimal total = lineItem.total();

        // Assert
        // Taxable: 450, Tax: 36, Total: 486
        assertThat(total).isEqualByComparingTo(new BigDecimal("486.00"));
    }

    @Test
    @DisplayName("Should throw exception for null description")
    void shouldThrowExceptionForNullDescription() {
        // Act & Assert
        assertThatThrownBy(() -> new LineItem(
            "1",
            null,
            5,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Description is required");
    }

    @Test
    @DisplayName("Should throw exception for empty description")
    void shouldThrowExceptionForEmptyDescription() {
        // Act & Assert
        assertThatThrownBy(() -> new LineItem(
            "1",
            "   ",
            5,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Description is required");
    }

    @Test
    @DisplayName("Should throw exception for zero quantity")
    void shouldThrowExceptionForZeroQuantity() {
        // Act & Assert
        assertThatThrownBy(() -> new LineItem(
            "1",
            "Item",
            0,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Quantity must be positive");
    }

    @Test
    @DisplayName("Should throw exception for negative quantity")
    void shouldThrowExceptionForNegativeQuantity() {
        // Act & Assert
        assertThatThrownBy(() -> new LineItem(
            "1",
            "Item",
            -5,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Quantity must be positive");
    }

    @Test
    @DisplayName("Should throw exception for negative unit price")
    void shouldThrowExceptionForNegativeUnitPrice() {
        // Act & Assert
        assertThatThrownBy(() -> new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("-100.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unit price cannot be negative");
    }

    @Test
    @DisplayName("Should throw exception for discount percent less than 0")
    void shouldThrowExceptionForNegativeDiscount() {
        // Act & Assert
        assertThatThrownBy(() -> new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            new BigDecimal("-0.1"),
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Discount percent must be between 0 and 1");
    }

    @Test
    @DisplayName("Should throw exception for discount percent greater than 1")
    void shouldThrowExceptionForDiscountGreaterThanOne() {
        // Act & Assert
        assertThatThrownBy(() -> new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            new BigDecimal("1.5"),
            BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Discount percent must be between 0 and 1");
    }

    @Test
    @DisplayName("Should throw exception for negative tax rate")
    void shouldThrowExceptionForNegativeTaxRate() {
        // Act & Assert
        assertThatThrownBy(() -> new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            new BigDecimal("-0.08")
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Tax rate cannot be negative");
    }

    @Test
    @DisplayName("Should default discount to zero if null")
    void shouldDefaultDiscountToZero() {
        // Arrange & Act
        LineItem lineItem = new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            null,
            BigDecimal.ZERO
        );

        // Assert
        assertThat(lineItem.discountPercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(lineItem.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should default tax rate to zero if null")
    void shouldDefaultTaxRateToZero() {
        // Arrange & Act
        LineItem lineItem = new LineItem(
            "1",
            "Item",
            5,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            null
        );

        // Assert
        assertThat(lineItem.taxRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(lineItem.taxAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
