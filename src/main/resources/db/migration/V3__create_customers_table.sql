-- Create customers table
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    street VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(50),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT uk_customer_email UNIQUE (email)
);

-- Create index on email for faster lookups
CREATE INDEX idx_customer_email ON customers(email);

-- Create index on deleted_at for filtering soft-deleted records
CREATE INDEX idx_customer_deleted_at ON customers(deleted_at);

-- Add comment for documentation
COMMENT ON TABLE customers IS 'Customer aggregate root - stores customer information with soft delete support';
COMMENT ON COLUMN customers.deleted_at IS 'Soft delete timestamp - NULL for active customers, set for deleted ones';
