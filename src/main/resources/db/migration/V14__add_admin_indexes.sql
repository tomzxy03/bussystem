CREATE INDEX idx_bookings_payment_created
ON bookings(payment_status, created_at)
WHERE deleted_at IS NULL AND payment_status = 'PAID';

CREATE INDEX idx_bookings_status_created
ON bookings(status, created_at DESC)
WHERE deleted_at IS NULL;

CREATE INDEX idx_trips_status_date
ON trips(status, departure_date)
WHERE deleted_at IS NULL;

CREATE INDEX idx_users_status_role_banned
ON users(status, role, is_banned, created_at DESC)
WHERE deleted_at IS NULL;

CREATE INDEX idx_users_username_lower
ON users(LOWER(username))
WHERE deleted_at IS NULL;

CREATE INDEX idx_users_email_lower
ON users(LOWER(email))
WHERE deleted_at IS NULL;

CREATE INDEX idx_users_full_name_lower
ON users(LOWER(full_name))
WHERE deleted_at IS NULL;
