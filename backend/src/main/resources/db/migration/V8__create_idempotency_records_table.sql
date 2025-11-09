-- Create idempotency_records table for preventing duplicate request processing
CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    result TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

-- Create index on idempotency_key for fast lookup
CREATE UNIQUE INDEX idx_idempotency_key ON idempotency_records(idempotency_key);

-- Create index on expires_at for cleanup queries
CREATE INDEX idx_idempotency_expires_at ON idempotency_records(expires_at);

-- Add comment to table
COMMENT ON TABLE idempotency_records IS 'Stores idempotency keys and cached results to prevent duplicate request processing';
COMMENT ON COLUMN idempotency_records.idempotency_key IS 'Unique key provided by client to identify duplicate requests';
COMMENT ON COLUMN idempotency_records.result IS 'JSON serialized result of the original request';
COMMENT ON COLUMN idempotency_records.expires_at IS 'Timestamp when this record should be cleaned up';
