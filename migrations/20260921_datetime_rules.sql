-- Run before deploying the updated application. Existing schedules and booking prices remain unchanged.
BEGIN;
ALTER TABLE showtimes ADD COLUMN original_duration_micros BIGINT;
UPDATE showtimes SET original_duration_micros = (EXTRACT(EPOCH FROM (end_time - start_time)) * 1000000)::BIGINT;
ALTER TABLE showtimes ALTER COLUMN original_duration_micros SET NOT NULL;
ALTER TABLE showtimes ADD CONSTRAINT chk_original_duration CHECK (original_duration_micros > 0);
ALTER TABLE bookings ADD COLUMN late_discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (late_discount_amount >= 0);
COMMIT;
