CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE RESTRICT,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_reviews_booking UNIQUE (booking_id)
);

CREATE INDEX idx_reviews_trip_created
    ON reviews(trip_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_reviews_user
    ON reviews(user_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX idx_reviews_booking_unique
    ON reviews(booking_id)
    WHERE deleted_at IS NULL;

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_status
    ON notifications(user_id, status)
    WHERE status <> 'READ';

CREATE INDEX idx_notifications_pending_retry
    ON notifications(status, retry_count)
    WHERE status IN ('PENDING', 'FAILED') AND retry_count < 3;
