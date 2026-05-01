ALTER TABLE users
    ADD COLUMN role VARCHAR(20);

UPDATE users
SET role = CASE
    WHEN user_type = 'VENDOR' THEN 'VENDOR'
    ELSE 'CUSTOMER'
END
WHERE role IS NULL;

ALTER TABLE users
    ALTER COLUMN role SET NOT NULL,
    ALTER COLUMN role SET DEFAULT 'CUSTOMER';

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_user_type_check;

ALTER TABLE users
    ADD CONSTRAINT chk_users_user_type
        CHECK (user_type IN ('CUSTOMER', 'VENDOR', 'PLATFORM_ADMIN'));

ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('CUSTOMER', 'VENDOR', 'PLATFORM_ADMIN'));

CREATE INDEX idx_users_role ON users (role);
