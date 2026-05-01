-- V2: Create users table (Phase 1 – Auth Module)

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    date_of_birth   DATE,
    gender          VARCHAR(10),
    avatar_url      TEXT,
    address         TEXT,
    company_id      BIGINT,
    user_type       VARCHAR(20)  DEFAULT 'CUSTOMER' CHECK(user_type IN ('CUSTOMER', 'VENDOR')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMPTZ,
    last_login_at   TIMESTAMPTZ,
    last_login_ip   VARCHAR(45),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

-- Partial unique indexes: ignore soft-deleted rows
CREATE UNIQUE INDEX uq_users_username_active ON users (username) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_email_active    ON users (email)    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_phone_active    ON users (phone)    WHERE deleted_at IS NULL AND phone IS NOT NULL;

-- General indexes
CREATE INDEX idx_users_status      ON users (status);
CREATE INDEX idx_users_company_active ON users(company_id);
CREATE INDEX idx_users_deleted_at  ON users (deleted_at);
