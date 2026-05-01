-- Phase 4: Route & Pricing tables

-- Routes (belongs to Company, soft delete)
CREATE TABLE routes (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    distance_km DECIMAL(10,2) NOT NULL,
    duration_minutes INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX idx_routes_code_active ON routes(code) WHERE deleted_at IS NULL;
CREATE INDEX idx_routes_company_active ON routes(company_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_routes_name ON routes USING gin(to_tsvector('vietnamese', name));

-- Route Stops (ordered sequence of stops)
CREATE TABLE route_stops (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    stop_id BIGINT NOT NULL REFERENCES stops(id) ON DELETE RESTRICT,
    stop_order INT NOT NULL,
    estimated_minutes_from_origin INT NOT NULL,
    distance_from_origin DECIMAL(10,2) NOT NULL,
    is_pickup BOOLEAN DEFAULT TRUE,
    is_dropoff BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(route_id, stop_order),
    UNIQUE(route_id, stop_id)
);
CREATE INDEX idx_route_stops_route_order ON route_stops(route_id, stop_order) WHERE deleted_at IS NULL;

-- Route Prices (segment pricing using ORDER, NOT stop_id)
CREATE TABLE route_prices (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    pickup_order INT NOT NULL,
    dropoff_order INT NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK (price > 0),
    currency VARCHAR(3) DEFAULT 'VND',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(route_id, pickup_order, dropoff_order),
    CONSTRAINT chk_price_orders CHECK (pickup_order < dropoff_order)
);
CREATE INDEX idx_route_prices_route_segment ON route_prices(route_id, pickup_order, dropoff_order)
    WHERE deleted_at IS NULL;
