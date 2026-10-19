-- Apply after 20261017_02_payment_refund_tickets.sql, before enabling stripe.enabled.
BEGIN;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_payment_method_check;
ALTER TABLE bookings ADD CONSTRAINT bookings_payment_method_check CHECK (payment_method IN ('CASH','VNPAY','MOMO','STRIPE'));
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_payment_method_check;
ALTER TABLE invoices ADD CONSTRAINT invoices_payment_method_check CHECK (payment_method IN ('CASH','VNPAY','MOMO','STRIPE'));

-- One durable creation per booking. Checkout and API-driver modes cannot be mixed.
CREATE TABLE stripe_payments (
    booking_id INT PRIMARY KEY REFERENCES bookings(booking_id),
    payment_request_id INT NOT NULL UNIQUE REFERENCES payment_requests(payment_request_id),
    mode VARCHAR(16) NOT NULL CHECK (mode IN ('CHECKOUT','API')),
    amount BIGINT NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL CHECK (currency = 'vnd'),
    account_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    session_id VARCHAR(255) UNIQUE,
    intent_id VARCHAR(255) UNIQUE,
    pay_url TEXT,
    state VARCHAR(30) NOT NULL DEFAULT 'CREATED' CHECK (state IN ('CREATED','SUCCEEDED','EXPIRED','REVIEW_REQUIRED')),
    deadline TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    success_url TEXT NOT NULL,
    cancel_url TEXT NOT NULL,
    scenario VARCHAR(100),
    CHECK ((mode='CHECKOUT' AND scenario IS NULL) OR
           (mode='API' AND scenario IN ('pm_card_visa','pm_card_visa_chargeDeclined','pm_card_visa_chargeDeclinedInsufficientFunds','pm_card_threeDSecure2Required')))
);
CREATE INDEX idx_stripe_payments_deadline ON stripe_payments(deadline) WHERE state='CREATED';
-- Only IDs/types retained; raw payloads may contain PII. Insert + business effects + processed_at
-- commit atomically. A failed handler rolls back the insert so Stripe delivery can retry.
CREATE TABLE stripe_webhook_inbox (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);
COMMIT;
