CREATE TABLE cancellation_policies (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id) ON DELETE CASCADE,
    route_id BIGINT REFERENCES routes(id) ON DELETE CASCADE,
    hours_before_departure INT NOT NULL,
    refund_percentage NUMERIC(5,2) NOT NULL CHECK (refund_percentage BETWEEN 0 AND 100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_cancellation_policies_scope
ON cancellation_policies(company_id, route_id)
WHERE deleted_at IS NULL AND is_active = TRUE;

CREATE TABLE cancellations (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT UNIQUE NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    cancelled_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    cancel_reason TEXT,
    cancel_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refund_amount NUMERIC(12,2) NOT NULL,
    refund_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    gateway_refund_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_refund_amount CHECK (refund_amount >= 0)
);

CREATE INDEX idx_cancellations_booking ON cancellations(booking_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cancellations_user ON cancellations(cancelled_by) WHERE deleted_at IS NULL;
CREATE INDEX idx_cancellations_refund_status ON cancellations(refund_status) WHERE deleted_at IS NULL;
