package com.invoiceme.customer.commands;

import com.invoiceme.customer.domain.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Command to update an existing customer
 * All fields except customerId are optional - only provided fields will be updated
 */
public record UpdateCustomerCommand(
    UUID customerId,

    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    String phone,

    @Valid
    AddressDto address
) {
    /**
     * Nested DTO for address data
     */
    public record AddressDto(
        String street,
        String city,
        String state,
        String postalCode,
        String country
    ) {
        /**
         * Converts DTO to domain value object
         */
        public Address toDomain() {
            return new Address(street, city, state, postalCode, country);
        }
    }

    /**
     * Converts address DTO to domain object, or returns null if no address provided
     */
    public Address getAddressDomain() {
        return address != null ? address.toDomain() : null;
    }

    /**
     * Checks if there are any updates to apply
     */
    public boolean hasUpdates() {
        return name != null || email != null || phone != null || address != null;
    }
}
