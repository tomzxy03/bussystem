-- Phase 8: Payment tables

-- Payment Methods (config table, no soft-delete)
CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,       -- COD, BANK_TRANSFER, MOMO, VNPAY
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    fee_percentage NUMERIC(4,2) DEFAULT 0.00,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed: default active methods
INSERT INTO payment_methods (code, name, is_active, fee_percentage) VALUES
('COD',           'Thanh toán tại quầy',       true,  0.00),
('BANK_TRANSFER', 'Chuyển khoản ngân hàng',     true,  0.00),
('MOMO',          'Ví điện tử MoMo',            false, 1.00),
('VNPAY',         'VNPAY – QR & Card',          false, 1.10);

-- Payments (one per booking, UNIQUE(booking_id))
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT UNIQUE NOT NULL REFERENCES bookings(id) ON DELETE RESTRICT,
    payment_method_id BIGINT NOT NULL REFERENCES payment_methods(id) ON DELETE RESTRICT,
    gateway_transaction_id VARCHAR(100),    -- Mã từ cổng (MoMo/VNPAY), NULL for COD
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'VND',
    status VARCHAR(20) DEFAULT 'PENDING',   -- PENDING, PAID, FAILED, REFUNDED, CANCELLED
    gateway_response JSONB,                 -- Raw callback payload for audit/debug
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(gateway_transaction_id)          -- Idempotency at DB level
);
CREATE INDEX idx_payments_booking ON payments(booking_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_payments_status ON payments(status) WHERE deleted_at IS NULL;
