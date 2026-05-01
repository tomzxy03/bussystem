-- Phase 5: Bus & Seat Layout tables

-- Bus Types (JSONB amenities via Hypersistence)
CREATE TABLE bus_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    amenities JSONB,
    base_price_per_km NUMERIC(10,2) NOT NULL DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_bus_types_code_active ON bus_types(code) WHERE deleted_at IS NULL;

-- Seat Layouts (JSONB layout_data)
CREATE TABLE seat_layouts (
    id BIGSERIAL PRIMARY KEY,
    bus_type_id BIGINT NOT NULL REFERENCES bus_types(id) ON DELETE RESTRICT,
    name VARCHAR(100) UNIQUE NOT NULL,
    layout_data JSONB NOT NULL,
    total_seats INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_seat_layouts_type ON seat_layouts(bus_type_id) WHERE deleted_at IS NULL;

-- Buses
CREATE TABLE buses (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    bus_type_id BIGINT NOT NULL REFERENCES bus_types(id) ON DELETE RESTRICT,
    seat_layout_id BIGINT REFERENCES seat_layouts(id) ON DELETE SET NULL,
    license_plate VARCHAR(20) UNIQUE NOT NULL,
    bus_number VARCHAR(50),
    name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_buses_plate_active ON buses(license_plate) WHERE deleted_at IS NULL;
CREATE INDEX idx_buses_company_active ON buses(company_id) WHERE deleted_at IS NULL;

-- Seats (generated from layout or manually)
CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    bus_id BIGINT NOT NULL REFERENCES buses(id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) DEFAULT 'STANDARD',
    row_num INT,
    col_num INT,
    price_multiplier NUMERIC(4,2) DEFAULT 1.00,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(bus_id, seat_number)
);
CREATE INDEX idx_seats_bus_active ON seats(bus_id) WHERE deleted_at IS NULL;
