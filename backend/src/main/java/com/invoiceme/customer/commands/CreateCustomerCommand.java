package com.invoiceme.customer.commands;

import com.invoiceme.customer.domain.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command to create a new customer
 */
public record CreateCustomerCommand(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Email is required")
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
        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        @NotBlank(message = "Country is required")
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
}
