package com.invoiceme.customer.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    @Test
    void shouldCreateCustomerWithValidData() {
        // Arrange & Act
        Customer customer = Customer.create(
            "John Doe",
            "john@example.com",
            "555-1234",
            new Address("123 Main St", "Springfield", "IL", "62701", "USA")
        );

        // Assert
        assertNotNull(customer);
        assertEquals("John Doe", customer.getName());
        assertEquals("john@example.com", customer.getEmail());
        assertEquals("555-1234", customer.getPhone());
        assertNotNull(customer.getAddress());
        assertNull(customer.getDeletedAt());
        assertFalse(customer.isDeleted());
    }

    @Test
    void shouldCreateCustomerWithMinimalData() {
        // Arrange & Act
        Customer customer = Customer.create("Jane Doe", "jane@example.com", null, null);

        // Assert
        assertNotNull(customer);
        assertEquals("Jane Doe", customer.getName());
        assertEquals("jane@example.com", customer.getEmail());
        assertNull(customer.getPhone());
        assertNull(customer.getAddress());
    }

    @Test
    void shouldNormalizeEmailToLowercase() {
        // Arrange & Act
        Customer customer = Customer.create("John Doe", "JOHN@EXAMPLE.COM", null, null);

        // Assert
        assertEquals("john@example.com", customer.getEmail());
    }

    @Test
    void shouldTrimNameAndEmail() {
        // Arrange & Act
        Customer customer = Customer.create("  John Doe  ", "  john@example.com  ", null, null);

        // Assert
        assertEquals("John Doe", customer.getName());
        assertEquals("john@example.com", customer.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Customer.create(null, "john@example.com", null, null)
        );
        assertEquals("Name is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNameIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Customer.create("   ", "john@example.com", null, null)
        );
        assertEquals("Name is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNameExceedsMaxLength() {
        // Arrange
        String longName = "a".repeat(256);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Customer.create(longName, "john@example.com", null, null)
        );
        assertEquals("Name must not exceed 255 characters", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Customer.create("John Doe", null, null, null)
        );
        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Customer.create("John Doe", "   ", null, null)
        );
        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailFormatIsInvalid() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Customer.create("John Doe", "invalid-email", null, null)
        );
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailExceedsMaxLength() {
        // Arrange
        String longEmail = "a".repeat(250) + "@example.com";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Customer.create("John Doe", longEmail, null, null)
        );
        assertEquals("Email must not exceed 255 characters", exception.getMessage());
    }

    @Test
    void shouldAcceptValidEmailFormats() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> Customer.create("John", "john@example.com", null, null));
        assertDoesNotThrow(() -> Customer.create("John", "john.doe@example.com", null, null));
        assertDoesNotThrow(() -> Customer.create("John", "john+tag@example.co.uk", null, null));
        assertDoesNotThrow(() -> Customer.create("John", "john_doe@example-site.com", null, null));
    }

    @Test
    void shouldThrowExceptionWhenPhoneExceedsMaxLength() {
        // Arrange
        String longPhone = "1".repeat(51);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Customer.create("John Doe", "john@example.com", longPhone, null)
        );
        assertEquals("Phone must not exceed 50 characters", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAddressIsInvalid() {
        // Arrange
        Address invalidAddress = new Address(null, "Springfield", "IL", "62701", "USA");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Customer.create("John Doe", "john@example.com", null, invalidAddress)
        );
        assertEquals("Street is required", exception.getMessage());
    }

    @Test
    void shouldUpdateCustomerName() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);

        // Act
        customer.update("Jane Doe", null, null, null);

        // Assert
        assertEquals("Jane Doe", customer.getName());
        assertEquals("john@example.com", customer.getEmail());
    }

    @Test
    void shouldUpdateCustomerEmail() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);

        // Act
        customer.update(null, "jane@example.com", null, null);

        // Assert
        assertEquals("John Doe", customer.getName());
        assertEquals("jane@example.com", customer.getEmail());
    }

    @Test
    void shouldUpdateCustomerPhone() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", "555-1234", null);

        // Act
        customer.update(null, null, "555-5678", null);

        // Assert
        assertEquals("555-5678", customer.getPhone());
    }

    @Test
    void shouldUpdateCustomerAddress() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);
        Address newAddress = new Address("456 Oak Ave", "Chicago", "IL", "60601", "USA");

        // Act
        customer.update(null, null, null, newAddress);

        // Assert
        assertEquals("456 Oak Ave", customer.getAddress().getStreet());
    }

    @Test
    void shouldUpdateMultipleFields() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);
        Address newAddress = new Address("456 Oak Ave", "Chicago", "IL", "60601", "USA");

        // Act
        customer.update("Jane Smith", "jane@example.com", "555-9999", newAddress);

        // Assert
        assertEquals("Jane Smith", customer.getName());
        assertEquals("jane@example.com", customer.getEmail());
        assertEquals("555-9999", customer.getPhone());
        assertEquals("456 Oak Ave", customer.getAddress().getStreet());
    }

    @Test
    void shouldThrowExceptionWhenUpdateWithInvalidName() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> customer.update("", null, null, null)
        );
        assertEquals("Name is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUpdateWithInvalidEmail() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> customer.update(null, "invalid-email", null, null)
        );
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void shouldSoftDeleteCustomer() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);
        assertFalse(customer.isDeleted());

        // Act
        customer.delete();

        // Assert
        assertTrue(customer.isDeleted());
        assertNotNull(customer.getDeletedAt());
    }

    @Test
    void shouldReturnFalseForIsDeletedWhenNotDeleted() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);

        // Act & Assert
        assertFalse(customer.isDeleted());
    }

    @Test
    void shouldReturnTrueForIsDeletedWhenDeleted() {
        // Arrange
        Customer customer = Customer.create("John Doe", "john@example.com", null, null);

        // Act
        customer.delete();

        // Assert
        assertTrue(customer.isDeleted());
    }
}
