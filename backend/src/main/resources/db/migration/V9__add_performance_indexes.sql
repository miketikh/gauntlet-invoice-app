-- V9: Add Performance Indexes
-- This migration adds indexes to improve query performance based on common access patterns

-- Note: users table only has id, username, password_hash, created_at
-- No email or role columns exist, so those indexes are skipped

-- Indexes on customers table
-- Note: email already has index from V3, but we'll create IF NOT EXISTS for consistency
CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name);
CREATE INDEX IF NOT EXISTS idx_customers_email_v9 ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers(created_at DESC);

-- Composite index for customer search (common pattern)
CREATE INDEX IF NOT EXISTS idx_customers_name_email ON customers(name, email);

-- Indexes on invoices table
CREATE INDEX IF NOT EXISTS idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_issue_date ON invoices(issue_date DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_due_date ON invoices(due_date);
CREATE INDEX IF NOT EXISTS idx_invoices_created_at ON invoices(created_at DESC);

-- Composite indexes for common invoice queries
CREATE INDEX IF NOT EXISTS idx_invoices_customer_status ON invoices(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_customer_date ON invoices(customer_id, issue_date DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_status_due_date ON invoices(status, due_date);

-- Indexes on payments table
CREATE INDEX IF NOT EXISTS idx_payments_invoice_id ON payments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payments_payment_date ON payments(payment_date DESC);
CREATE INDEX IF NOT EXISTS idx_payments_payment_method ON payments(payment_method);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at DESC);

-- Composite index for payment queries by invoice and date
CREATE INDEX IF NOT EXISTS idx_payments_invoice_date ON payments(invoice_id, payment_date DESC);

-- Indexes on idempotency_records table
CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_records(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_idempotency_created_at ON idempotency_records(created_at);

-- Partial indexes for active/pending records (PostgreSQL specific optimization)
-- Note: Invoice status values are PascalCase: 'Draft', 'Sent', 'Paid'
CREATE INDEX IF NOT EXISTS idx_invoices_status_draft ON invoices(customer_id, created_at DESC) WHERE status = 'Draft';
CREATE INDEX IF NOT EXISTS idx_invoices_status_sent ON invoices(customer_id, due_date) WHERE status = 'Sent';
-- Note: Overdue check requires CURRENT_DATE which is volatile, so we use a composite index instead
CREATE INDEX IF NOT EXISTS idx_invoices_sent_due_date ON invoices(due_date, customer_id) WHERE status = 'Sent';

-- Add comments for documentation
COMMENT ON INDEX idx_customers_name_email IS 'Optimizes customer search by name and email';
COMMENT ON INDEX idx_invoices_customer_status IS 'Optimizes queries filtering by customer and status';
COMMENT ON INDEX idx_invoices_sent_due_date IS 'Optimizes overdue invoice queries (filter on due_date < CURRENT_DATE in application)';
COMMENT ON INDEX idx_payments_invoice_date IS 'Optimizes payment history queries by invoice';
