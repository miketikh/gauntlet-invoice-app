package com.invoiceme.customer.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressTest {

    @Test
    void shouldCreateValidAddress() {
        // Arrange & Act
        Address address = new Address("123 Main St", "Springfield", "IL", "62701", "USA");

        // Assert
        assertNotNull(address);
        assertEquals("123 Main St", address.getStreet());
        assertEquals("Springfield", address.getCity());
        assertEquals("IL", address.getState());
        assertEquals("62701", address.getPostalCode());
        assertEquals("USA", address.getCountry());
    }

    @Test
    void shouldValidateSuccessfullyWithAllFields() {
        // Arrange
        Address address = new Address("123 Main St", "Springfield", "IL", "62701", "USA");

        // Act & Assert
        assertDoesNotThrow(address::validate);
    }

    @Test
    void shouldThrowExceptionWhenStreetIsNull() {
        // Arrange
        Address address = new Address(null, "Springfield", "IL", "62701", "USA");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            address::validate
        );
        assertEquals("Street is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenStreetIsEmpty() {
        // Arrange
        Address address = new Address("  ", "Springfield", "IL", "62701", "USA");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            address::validate
        );
        assertEquals("Street is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenCityIsNull() {
        // Arrange
        Address address = new Address("123 Main St", null, "IL", "62701", "USA");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            address::validate
        );
        assertEquals("City is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenStateIsNull() {
        // Arrange
        Address address = new Address("123 Main St", "Springfield", null, "62701", "USA");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            address::validate
        );
        assertEquals("State is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenPostalCodeIsNull() {
        // Arrange
        Address address = new Address("123 Main St", "Springfield", "IL", null, "USA");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            address::validate
        );
        assertEquals("Postal code is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenCountryIsNull() {
        // Arrange
        Address address = new Address("123 Main St", "Springfield", "IL", "62701", null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            address::validate
        );
        assertEquals("Country is required", exception.getMessage());
    }

    @Test
    void shouldHaveEqualityBasedOnAllFields() {
        // Arrange
        Address address1 = new Address("123 Main St", "Springfield", "IL", "62701", "USA");
        Address address2 = new Address("123 Main St", "Springfield", "IL", "62701", "USA");
        Address address3 = new Address("456 Oak Ave", "Springfield", "IL", "62701", "USA");

        // Act & Assert
        assertEquals(address1, address2);
        assertNotEquals(address1, address3);
    }

    @Test
    void shouldHaveConsistentHashCode() {
        // Arrange
        Address address1 = new Address("123 Main St", "Springfield", "IL", "62701", "USA");
        Address address2 = new Address("123 Main St", "Springfield", "IL", "62701", "USA");

        // Act & Assert
        assertEquals(address1.hashCode(), address2.hashCode());
    }

    @Test
    void shouldFormatToStringCorrectly() {
        // Arrange
        Address address = new Address("123 Main St", "Springfield", "IL", "62701", "USA");

        // Act
        String result = address.toString();

        // Assert
        assertEquals("123 Main St, Springfield, IL 62701, USA", result);
    }
}
