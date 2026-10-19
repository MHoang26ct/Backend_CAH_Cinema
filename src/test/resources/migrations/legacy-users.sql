-- Snapshot of users before 20261017_01_account_type.sql (not the new schema).
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    provider_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    password VARCHAR(255),
    phone VARCHAR(20),
    avatar_url TEXT,
    auth_provider VARCHAR(20) NOT NULL CHECK (auth_provider IN ('EMAIL', 'GOOGLE')),
    total_paid DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (total_paid >= 0),
    total_point INT NOT NULL DEFAULT 0 CHECK (total_point >= 0),
    rank_level VARCHAR(20) NOT NULL DEFAULT 'SILVER' CHECK (rank_level IN ('SILVER', 'GOLD', 'DIAMOND')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);
