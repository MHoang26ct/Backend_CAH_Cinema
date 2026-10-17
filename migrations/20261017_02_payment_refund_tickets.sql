-- Migration: Batch 2 - Add payment receipts, refunds, ticket status, booking deadlines & snapshots
-- Preflight:
--   1. Backup tables: tickets, bookings, payment_requests, payment_confirmations.
--   2. Verify that tickets does not contain duplicate VALID tickets before creating the partial index.
-- Rollback note:
--   - Additive columns and tables can remain.
--   - If rolling back, do NOT drop columns while newer code is running.
BEGIN;
SET LOCAL lock_timeout = '5s';

-- 1. Ticket status & partial unique index for cancellation / resale support
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'VALID'
    CONSTRAINT chk_tickets_status CHECK (status IN ('VALID', 'CANCELLED'));

-- Backfill cancelled tickets based on existing cancelled bookings
UPDATE tickets SET status = 'CANCELLED'
WHERE booking_id IN (SELECT booking_id FROM bookings WHERE status IN ('CANCELLED', 'EXPIRED'));

-- Create partial unique index allowing only one VALID ticket per (showtime, seat)
CREATE UNIQUE INDEX IF NOT EXISTS uq_tickets_valid_showtime_seat
    ON tickets (showtime_id, seat_id) WHERE status = 'VALID';

-- Drop the old unconditional unique constraint if it exists
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS uq_tickets_showtime_seat;

-- 2. Bookings: deadlines, actor source, contact snapshot, price snapshot breakdown, food fulfillment, idempotency markers
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS payment_deadline TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reconciliation_deadline TIMESTAMP,
    ADD COLUMN IF NOT EXISTS booking_source VARCHAR(20) NOT NULL DEFAULT 'ONLINE'
        CONSTRAINT chk_bookings_source CHECK (booking_source IN ('ONLINE', 'COUNTER')),
    ADD COLUMN IF NOT EXISTS created_by_staff_id INT REFERENCES users(user_id),
    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS seat_subtotal_gross DECIMAL(18,2) DEFAULT 0 CHECK (seat_subtotal_gross >= 0),
    ADD COLUMN IF NOT EXISTS food_subtotal_gross DECIMAL(18,2) DEFAULT 0 CHECK (food_subtotal_gross >= 0),
    ADD COLUMN IF NOT EXISTS rank_discount_amount DECIMAL(18,2) DEFAULT 0 CHECK (rank_discount_amount >= 0),
    ADD COLUMN IF NOT EXISTS voucher_discount_seat DECIMAL(18,2) DEFAULT 0 CHECK (voucher_discount_seat >= 0),
    ADD COLUMN IF NOT EXISTS voucher_discount_food DECIMAL(18,2) DEFAULT 0 CHECK (voucher_discount_food >= 0),
    ADD COLUMN IF NOT EXISTS net_seat_amount DECIMAL(18,2) DEFAULT 0 CHECK (net_seat_amount >= 0),
    ADD COLUMN IF NOT EXISTS net_food_amount DECIMAL(18,2) DEFAULT 0 CHECK (net_food_amount >= 0),
    ADD COLUMN IF NOT EXISTS food_fulfillment_status VARCHAR(20) NOT NULL DEFAULT 'NOT_APPLICABLE'
        CONSTRAINT chk_bookings_food_fulfillment CHECK (food_fulfillment_status IN ('NOT_APPLICABLE', 'PENDING', 'FULFILLED')),
    ADD COLUMN IF NOT EXISTS food_fulfilled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS food_fulfilled_by_staff_id INT REFERENCES users(user_id),
    ADD COLUMN IF NOT EXISTS cancellation_policy_snapshot TEXT,
    ADD COLUMN IF NOT EXISTS is_voucher_released BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_points_accumulated BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_points_reversed BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill booking deadlines from expires_at if null
UPDATE bookings SET payment_deadline = expires_at WHERE payment_deadline IS NULL;
UPDATE bookings SET reconciliation_deadline = expires_at WHERE reconciliation_deadline IS NULL;
UPDATE bookings SET net_seat_amount = total_price WHERE net_seat_amount = 0 AND total_price > 0;
UPDATE bookings SET food_fulfillment_status = 'FULFILLED'
    WHERE status = 'CHECKED_IN' AND EXISTS (SELECT 1 FROM food_orders fo WHERE fo.booking_id = bookings.booking_id);
UPDATE bookings SET food_fulfillment_status = 'PENDING'
    WHERE status = 'PAID' AND EXISTS (SELECT 1 FROM food_orders fo WHERE fo.booking_id = bookings.booking_id);

-- 3. Payment requests: audit and gateway date/info fields
ALTER TABLE payment_requests
    ADD COLUMN IF NOT EXISTS vnp_create_date VARCHAR(14),
    ADD COLUMN IF NOT EXISTS vnp_expire_date VARCHAR(14),
    ADD COLUMN IF NOT EXISTS vnp_pay_date VARCHAR(14),
    ADD COLUMN IF NOT EXISTS vnp_bank_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS vnp_bank_tran_no VARCHAR(255),
    ADD COLUMN IF NOT EXISTS vnp_card_type VARCHAR(50);

-- 4. Payment receipts: record verified payment transactions independently from ticket issuance
CREATE TABLE IF NOT EXISTS payment_receipts (
    payment_receipt_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    payment_request_id INT REFERENCES payment_requests(payment_request_id),
    gateway VARCHAR(30) NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL,
    order_id VARCHAR(200),
    amount DECIMAL(18,2) NOT NULL CHECK (amount >= 0),
    pay_date TIMESTAMP,
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'RECORDED'
        CHECK (status IN ('RECORDED', 'ALLOCATED', 'REFUNDED', 'OVERPAID')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_receipts_gateway_trans UNIQUE (gateway, transaction_reference)
);

CREATE INDEX IF NOT EXISTS idx_payment_receipts_booking ON payment_receipts (booking_id);

-- 5. Refunds: record refund obligations, statuses and gateway responses
CREATE TABLE IF NOT EXISTS refunds (
    refund_id SERIAL PRIMARY KEY,
    payment_receipt_id INT NOT NULL REFERENCES payment_receipts(payment_receipt_id),
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    cause VARCHAR(50) NOT NULL
        CHECK (cause IN ('CUSTOMER_CANCELLED', 'SHOWTIME_CANCELLED', 'EXPIRED_LATE_PAYMENT', 'DUPLICATE_CHARGE', 'SEAT_ALLOCATION_FAILED', 'MANUAL_STAFF')),
    amount DECIMAL(18,2) NOT NULL CHECK (amount > 0),
    policy_snapshot TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED'
        CHECK (status IN ('REQUESTED', 'PROCESSING', 'UNKNOWN', 'SUCCEEDED', 'FAILED', 'REVIEW_REQUIRED')),
    request_reference VARCHAR(100) NOT NULL,
    actor_staff_id INT REFERENCES users(user_id),
    claimed_at TIMESTAMP,
    processed_at TIMESTAMP,
    gateway_response_code VARCHAR(50),
    gateway_transaction_no VARCHAR(100),
    gateway_response_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_refunds_receipt_cause UNIQUE (payment_receipt_id, cause),
    CONSTRAINT uq_refunds_request_ref UNIQUE (request_reference)
);

CREATE INDEX IF NOT EXISTS idx_refunds_booking ON refunds (booking_id);
CREATE INDEX IF NOT EXISTS idx_refunds_status ON refunds (status);

COMMIT;
