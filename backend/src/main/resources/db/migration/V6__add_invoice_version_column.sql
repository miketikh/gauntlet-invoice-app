-- Add version column for optimistic locking to invoices table
ALTER TABLE invoices ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
