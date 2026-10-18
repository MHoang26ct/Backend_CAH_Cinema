-- Apply after 20261017_03_stripe_sandbox.sql.
-- Older deployed schemas also have manually named checks in addition to PostgreSQL's default names.
-- Keep the canonical checks installed by migration 03; remove only the redundant old gateway allowlists.
BEGIN;
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS chk_bookings_payment_method;
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS chk_invoices_payment_method;
COMMIT;
