USE dpdms_auth;
CREATE TABLE IF NOT EXISTS users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 username VARCHAR(150) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 role VARCHAR(80) NOT NULL,
 hazard VARCHAR(40) NOT NULL,
 ward VARCHAR(100),
 district VARCHAR(100),
 province VARCHAR(100),
 enabled BOOLEAN NOT NULL DEFAULT TRUE
);
-- Passwords are seeded by AuthServiceApplication using BCrypt so no plaintext password is stored in SQL.
