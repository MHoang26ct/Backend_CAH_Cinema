-- Apply after 20261017_02_payment_refund_tickets.sql.
-- Per-refund audit markers make member spend/point adjustments idempotent and reversible
-- if a provider later corrects a previously successful refund to FAILED.
BEGIN;

ALTER TABLE refunds
    ADD COLUMN IF NOT EXISTS membership_adjusted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS membership_adjustment_reversed_at TIMESTAMP;

COMMIT;
