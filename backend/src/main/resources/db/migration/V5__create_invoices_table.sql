-- Create invoices table with JSONB line items
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('Draft', 'Sent', 'Paid')),
    payment_terms VARCHAR(100),

    -- Line items stored as JSONB for aggregate persistence
    line_items JSONB NOT NULL DEFAULT '[]'::jsonb,

    -- Calculated totals (denormalized for query performance)
    subtotal DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_discount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_tax DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0,

    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_total_amount_non_negative CHECK (total_amount >= 0),
    CONSTRAINT chk_due_date_after_issue CHECK (due_date >= issue_date)
);

-- Create indexes for query performance
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_invoice_number ON invoices(invoice_number);

-- Create sequence for invoice number generation
CREATE SEQUENCE invoice_number_seq START 1;

-- Create function to generate invoice numbers
-- Format: INV-{YEAR}-{SEQUENCE} (e.g., INV-2024-0001)
CREATE OR REPLACE FUNCTION generate_invoice_number()
RETURNS VARCHAR AS $$
BEGIN
    RETURN 'INV-' || EXTRACT(YEAR FROM CURRENT_DATE)::TEXT || '-' || LPAD(nextval('invoice_number_seq')::TEXT, 4, '0');
END;
$$ LANGUAGE plpgsql;

-- Add comment for documentation
COMMENT ON TABLE invoices IS 'Invoice aggregate root with embedded line items stored as JSONB';
COMMENT ON COLUMN invoices.line_items IS 'Line items stored as JSONB array for aggregate persistence';
COMMENT ON FUNCTION generate_invoice_number() IS 'Generates sequential invoice numbers in format INV-YYYY-####';
