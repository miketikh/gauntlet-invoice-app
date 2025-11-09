-- Update admin user password to correct BCrypt hash
-- Password: admin123
-- New BCrypt hash with 10 rounds (previous hash was incorrect)
UPDATE users
SET password_hash = '$2y$10$hSMUyXxpcpkHZVtmjchbv.erRUkZktWVd3BeoJJHZ.OjnYaz9Z7CK'
WHERE username = 'admin';
