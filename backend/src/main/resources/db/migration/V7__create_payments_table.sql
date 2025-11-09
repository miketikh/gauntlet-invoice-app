-- Create payments table
-- This table tracks payments applied to invoices
-- Each payment reduces the invoice balance and can trigger status changes

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,

    -- Foreign key constraint to ensure referential integrity
    CONSTRAINT fk_payments_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id)
        ON DELETE RESTRICT,  -- Prevent deleting invoices with payments

    -- Business rule constraints
    CONSTRAINT chk_payments_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_payments_method_valid
        CHECK (payment_method IN ('CREDIT_CARD', 'BANK_TRANSFER', 'CHECK', 'CASH'))
);

-- Index on invoice_id for fast lookup of payments by invoice
CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);

-- Index on payment_date for date range queries and reporting
CREATE INDEX idx_payments_payment_date ON payments(payment_date);

-- Comments for documentation
COMMENT ON TABLE payments IS 'Stores payment records applied to invoices';
COMMENT ON COLUMN payments.id IS 'Unique payment identifier (UUID)';
COMMENT ON COLUMN payments.invoice_id IS 'Foreign key to invoices table';
COMMENT ON COLUMN payments.payment_date IS 'Date the payment was made (cannot be in future)';
COMMENT ON COLUMN payments.amount IS 'Payment amount (must be positive, cannot exceed invoice balance)';
COMMENT ON COLUMN payments.payment_method IS 'Method of payment: CREDIT_CARD, BANK_TRANSFER, CHECK, or CASH';
COMMENT ON COLUMN payments.reference IS 'Optional reference number (transaction ID, check number, etc.)';
COMMENT ON COLUMN payments.notes IS 'Optional notes about the payment';
COMMENT ON COLUMN payments.created_at IS 'Timestamp when payment record was created';
COMMENT ON COLUMN payments.created_by IS 'User who recorded the payment';
