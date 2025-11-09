package com.invoiceme.customer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Customer Aggregate Root
 * Represents a customer entity with full business logic and validation
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_email", columnList = "email"),
    @Index(name = "idx_customer_deleted_at", columnList = "deletedAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Required by JPA
public class Customer {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Embedded
    private Address address;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    /**
     * Factory method to create a new Customer
     * @param name Customer full name
     * @param email Customer email address
     * @param phone Customer phone number (optional)
     * @param address Customer address (optional)
     * @return New Customer instance
     * @throws IllegalArgumentException if validation fails
     */
    public static Customer create(String name, String email, String phone, Address address) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setAddress(address);
        customer.validate();
        return customer;
    }

    /**
     * Updates customer information
     * @param name New name (optional)
     * @param email New email (optional)
     * @param phone New phone (optional)
     * @param address New address (optional)
     */
    public void update(String name, String email, String phone, Address address) {
        if (name != null) {
            this.setName(name);
        }
        if (email != null) {
            this.setEmail(email);
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (address != null) {
            this.setAddress(address);
        }
        this.validate();
    }

    /**
     * Soft deletes the customer by setting deletedAt timestamp
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Checks if customer is soft-deleted
     * @return true if customer is deleted
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Validates customer data
     * @throws IllegalArgumentException if validation fails
     */
    private void validate() {
        validateName();
        validateEmail();
        if (address != null) {
            address.validate();
        }
    }

    private void validateName() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Name must not exceed 255 characters");
        }
    }

    private void validateEmail() {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (email.length() > 255) {
            throw new IllegalArgumentException("Email must not exceed 255 characters");
        }
    }

    private void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    private void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    private void setPhone(String phone) {
        if (phone != null && phone.length() > 50) {
            throw new IllegalArgumentException("Phone must not exceed 50 characters");
        }
        this.phone = phone;
    }

    private void setAddress(Address address) {
        this.address = address;
    }
}
