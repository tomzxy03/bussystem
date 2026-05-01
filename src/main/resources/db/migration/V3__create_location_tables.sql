-- Phase 2: Location tables
-- Enable earthdistance extension (for geo-search)
CREATE EXTENSION IF NOT EXISTS earthdistance CASCADE;

-- Provinces (master data, rarely changes)
CREATE TABLE provinces (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    full_name VARCHAR(255),
    full_name_en VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_provinces_code_active ON provinces(code) WHERE deleted_at IS NULL;
CREATE INDEX idx_provinces_name ON provinces USING gin(to_tsvector('vietnamese', name));

-- Districts
CREATE TABLE districts (
    id BIGSERIAL PRIMARY KEY,
    province_id BIGINT NOT NULL REFERENCES provinces(id) ON DELETE RESTRICT,
    code VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100),
    full_name VARCHAR(255),
    full_name_en VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(province_id, code)
);
CREATE INDEX idx_districts_province_code ON districts(province_id, code) WHERE deleted_at IS NULL;
CREATE INDEX idx_districts_name ON districts USING gin(to_tsvector('vietnamese', name));

-- Stops
CREATE TABLE stops (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('STATION', 'OFFICE', 'ROAD_POINT', 'CUSTOM')),
    province_id BIGINT NOT NULL REFERENCES provinces(id) ON DELETE RESTRICT,
    district_id BIGINT NOT NULL REFERENCES districts(id) ON DELETE RESTRICT,
    address TEXT,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    is_major BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
-- Partial unique index on code (active records only)
CREATE UNIQUE INDEX idx_stops_code_active ON stops(code) WHERE code IS NOT NULL AND deleted_at IS NULL;
-- Geo index using earthdistance
CREATE INDEX idx_stops_location ON stops USING gist(ll_to_earth(latitude, longitude));
-- Full-text search on name + address
CREATE INDEX idx_stops_search ON stops USING gin(to_tsvector('vietnamese', name || ' ' || COALESCE(address, '')));
