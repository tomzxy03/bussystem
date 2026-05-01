-- Phase 3: Company & Driver tables

-- Companies (master data, soft delete)
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    tax_code VARCHAR(20) UNIQUE,
    phone VARCHAR(15) NOT NULL,
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
-- Partial unique index on tax_code (active only)
CREATE UNIQUE INDEX idx_companies_tax_code_active ON companies(tax_code)
    WHERE tax_code IS NOT NULL AND deleted_at IS NULL;
-- Full-text search index
CREATE INDEX idx_companies_name ON companies USING gin(to_tsvector('vietnamese', name));

-- Drivers (soft delete, belongs to company)
CREATE TABLE drivers (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    license_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    avatar_url VARCHAR(500),
    date_of_birth DATE,
    address TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(company_id, license_number),
    UNIQUE(company_id, phone)
);
-- Partial index for soft delete queries
CREATE INDEX idx_drivers_company_active ON drivers(company_id) WHERE deleted_at IS NULL;
-- Full-text search on name
CREATE INDEX idx_drivers_name ON drivers USING gin(to_tsvector('vietnamese', full_name));
