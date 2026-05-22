-- Run this in MySQL before starting the app
CREATE DATABASE IF NOT EXISTS executiondb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- If using root with existing password, just the DB creation is enough.
-- If you want a dedicated user instead:
-- CREATE USER IF NOT EXISTS 'execuser'@'localhost' IDENTIFIED BY 'yourpassword';
-- GRANT ALL PRIVILEGES ON executiondb.* TO 'execuser'@'localhost';
-- FLUSH PRIVILEGES;

USE executiondb;
SELECT 'Database executiondb ready.' AS status;
