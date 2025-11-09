-- Add default test user (password: admin123)
-- BCrypt hash of 'admin123' with 10 rounds
-- Note: password_hash column name from V1 schema
INSERT INTO users (username, password_hash)
VALUES ('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a');
