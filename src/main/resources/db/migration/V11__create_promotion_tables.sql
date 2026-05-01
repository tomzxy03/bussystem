-- Phase 9: Promotion tables

-- 1. Promotions master (soft delete)
CREATE TABLE promotions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,           -- e.g., "SUMMER2024"
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Discount config
    discount_type VARCHAR(20) NOT NULL,         -- PERCENTAGE, FIXED
    discount_value NUMERIC(10,2) NOT NULL,
    min_order_value NUMERIC(12,2) DEFAULT 0,
    max_discount NUMERIC(12,2),                 -- Cap for PERCENTAGE discounts

    -- Validity & usage
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ NOT NULL,
    usage_limit INT,                            -- NULL = unlimited
    used_count INT DEFAULT 0,
    per_user_limit INT DEFAULT 1,

    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT chk_discount_value CHECK (discount_value >= 0),
    CONSTRAINT chk_validity CHECK (valid_to >= valid_from)
);
-- Partial unique index: only one active code per code value
CREATE UNIQUE INDEX idx_promotions_code_active ON promotions(LOWER(code)) WHERE deleted_at IS NULL AND is_active = TRUE;
CREATE INDEX idx_promotions_validity ON promotions(valid_from, valid_to) WHERE deleted_at IS NULL AND is_active = TRUE;

-- 2. Promotion <-> Route M2M (which routes can use this promotion)
CREATE TABLE promotion_routes (
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    route_id BIGINT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (promotion_id, route_id)
);
CREATE INDEX idx_promotion_routes_route ON promotion_routes(route_id);

-- 3. Per-user usage tracking
CREATE TABLE user_promotion_usage (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    used_count INT DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    UNIQUE(user_id, promotion_id)
);
CREATE INDEX idx_user_promotion_usage_user ON user_promotion_usage(user_id);

-- 4. Extend bookings with promotion tracking columns
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS promotion_id BIGINT REFERENCES promotions(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(12,2) DEFAULT 0.00;
