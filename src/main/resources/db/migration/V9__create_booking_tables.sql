-- Phase 7: Booking tables

-- Bookings (main transaction)
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_code UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE RESTRICT,
    pickup_order INT NOT NULL,
    dropoff_order INT NOT NULL,
    base_price NUMERIC(12,2) NOT NULL,
    final_price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    payment_status VARCHAR(20) DEFAULT 'PENDING',   -- PENDING, PAID, REFUNDED, FAILED
    status VARCHAR(20) DEFAULT 'PENDING',           -- PENDING, CONFIRMED, CANCELLED, EXPIRED, COMPLETED
    reserved_until TIMESTAMPTZ,
    version BIGINT DEFAULT 0,                       -- Optimistic lock
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_booking_orders CHECK (pickup_order < dropoff_order)
);
CREATE INDEX idx_bookings_user ON bookings(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_bookings_trip_status ON bookings(trip_id, status) WHERE deleted_at IS NULL;
-- Partial index for expiry job query (only PENDING rows)
CREATE INDEX idx_bookings_reserved ON bookings(status, reserved_until)
    WHERE status = 'PENDING' AND deleted_at IS NULL;

-- Booking Seats (seat detail per booking)
CREATE TABLE booking_seats (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    seat_id BIGINT NOT NULL REFERENCES seats(id) ON DELETE RESTRICT,
    final_price NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(booking_id, seat_id)
);
CREATE INDEX idx_booking_seats_seat ON booking_seats(seat_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_booking_seats_seat_trip 
ON booking_seats(seat_id, booking_id) WHERE deleted_at IS NULL;

-- Passengers (1 per booking seat)
CREATE TABLE passengers (
    id BIGSERIAL PRIMARY KEY,
    booking_seat_id BIGINT UNIQUE NOT NULL REFERENCES booking_seats(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    id_card VARCHAR(20),
    date_of_birth DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);
