-- Phase 6: Trip & Availability tables

-- Trips (one scheduled run of a route on a bus)
CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE RESTRICT,
    bus_id BIGINT NOT NULL REFERENCES buses(id) ON DELETE RESTRICT,
    driver_id BIGINT REFERENCES drivers(id) ON DELETE SET NULL,
    departure_date DATE NOT NULL,
    departure_time TIMETZ NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT',   -- DRAFT, SCHEDULED, DEPARTED, COMPLETED, CANCELLED, DELAYED
    version BIGINT DEFAULT 0,             -- Optimistic lock
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(route_id, bus_id, departure_date, departure_time)
);
CREATE INDEX idx_trips_route_date ON trips(route_id, departure_date) WHERE deleted_at IS NULL;
CREATE INDEX idx_trips_bus_status ON trips(bus_id, status) WHERE deleted_at IS NULL;

-- Trip Segments (availability per pickup/dropoff order pair)
CREATE TABLE trip_segments (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    pickup_order INT NOT NULL,
    dropoff_order INT NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(trip_id, pickup_order, dropoff_order),
    CHECK (pickup_order < dropoff_order)
);
CREATE INDEX idx_trip_segments_trip_avail ON trip_segments(trip_id)
    WHERE deleted_at IS NULL AND available_seats > 0;
