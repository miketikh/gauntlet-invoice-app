-- V10: Update payment_method enum to use SCREAMING_SNAKE_CASE
-- Changes payment method values from PascalCase to match Java enum constants

-- Drop the old constraint
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_payment_method_check;

-- Add new constraint with SCREAMING_SNAKE_CASE values
ALTER TABLE payments ADD CONSTRAINT payments_payment_method_check
    CHECK (payment_method IN ('CREDIT_CARD', 'BANK_TRANSFER', 'CHECK', 'CASH'));

-- Add comment for documentation
COMMENT ON CONSTRAINT payments_payment_method_check ON payments IS
    'Ensures payment method matches Java enum constants (SCREAMING_SNAKE_CASE)';
