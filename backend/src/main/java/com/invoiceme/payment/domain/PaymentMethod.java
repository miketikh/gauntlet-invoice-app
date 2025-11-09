package com.invoiceme.payment.domain;

/**
 * PaymentMethod enum represents the different methods of payment accepted
 * Stored as STRING in database for readability and maintainability
 */
public enum PaymentMethod {
    /**
     * Payment made via credit card (Visa, Mastercard, Amex, etc.)
     */
    CREDIT_CARD("Credit Card"),

    /**
     * Payment made via direct bank transfer (ACH, wire transfer, etc.)
     */
    BANK_TRANSFER("Bank Transfer"),

    /**
     * Payment made via physical or electronic check
     */
    CHECK("Check"),

    /**
     * Payment made in cash
     */
    CASH("Cash");

    private final String displayName;

    /**
     * Constructor with display name
     * @param displayName Human-readable name for UI display
     */
    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the display name for UI representation
     * @return Display name string
     */
    public String getDisplayName() {
        return displayName;
    }
}
