-- Repair the seat-only placeholder written by 20261017_02; never rerun/edit that migration.
-- Historical evidence (pre-snapshot BookingService/TicketService/FoodOrderService):
--   tickets.price and food_orders.total_price are GROSS, not the amount paid.
--   discount_amount = late_discount_amount + voucher discount (excludes rank).
--   rank applied uniformly to seats/food; late discount was 50% of ranked seats,
--   HALF_UP to cents, and could not be combined with a voucher.
-- Historical rank factors were 1 (pre-rank), .98, .97, .95. Do NOT read today's
-- users.rank_level, vouchers, foods.price, seat multipliers or showtime prices.
-- Match persisted cents against those historical rules; do not infer a new rank
-- from an arbitrary ratio. A booking-wide voucher is allocated proportionally,
-- seat discount HALF_UP to cents, food receives the remainder. The final food net
-- receives any cent remainder so the two components equal the actual paid total.
-- If rounding admits multiple different splits, evidence is missing/inconsistent,
-- or the old total was clamped to zero after an excessive discount, quarantine.
--
-- DEPLOYMENT REQUIREMENT: quote AND cancel must reject REVIEW_REQUIRED before
-- calculating a partial refund or changing tickets/food/vouchers/booking state.
-- Full-receipt refunds (duplicate charge etc.) need not use these component splits.
-- NULL status means this migration did not classify the row, NOT proof of recovery.
-- Review requires historical evidence and an explicit audited manual resolution;
-- reruns deliberately do not clear quarantine or overwrite recovered/modern data.
-- Run in a maintenance window: locks make the evidence and candidate scan atomic.
BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS legacy_snapshot_status VARCHAR(30)
        CHECK (legacy_snapshot_status IN ('RECOVERED', 'REVIEW_REQUIRED')),
    ADD COLUMN IF NOT EXISTS legacy_snapshot_reason TEXT,
    ADD COLUMN IF NOT EXISTS legacy_snapshot_evidence JSONB;

LOCK TABLE tickets, food_orders, food_order_items, booking_food_draft_items IN SHARE MODE;

CREATE TEMP TABLE legacy_snapshot_repair ON COMMIT DROP AS
WITH candidates AS (
    SELECT b.*,
           t.ticket_count, t.seat_gross, t.bad_tickets,
           f.order_count, COALESCE(f.food_gross, 0) AS food_gross,
           f.bad_orders,
           COALESCE(d.draft_count, 0) AS food_draft_count
    FROM bookings b
    LEFT JOIN LATERAL (
        SELECT count(*) AS ticket_count, sum(price) AS seat_gross,
               count(*) FILTER (WHERE price IS NULL OR price < 0 OR showtime_id <> b.showtime_id) AS bad_tickets
        FROM tickets WHERE booking_id = b.booking_id
        -- Include CANCELLED tickets: these are immutable original purchase evidence.
    ) t ON TRUE
    LEFT JOIN LATERAL (
        SELECT count(*) AS order_count, sum(fo.total_price) AS food_gross,
               count(*) FILTER (WHERE fo.total_price IS NULL OR fo.total_price < 0
                   OR (EXISTS (SELECT 1 FROM food_order_items i WHERE i.food_order_id = fo.food_order_id)
                       AND fo.total_price <> (SELECT sum(i.price * i.quantity) FROM food_order_items i
                                             WHERE i.food_order_id = fo.food_order_id))) AS bad_orders
        FROM food_orders fo WHERE fo.booking_id = b.booking_id
    ) f ON TRUE
    LEFT JOIN LATERAL (
        SELECT count(*) AS draft_count FROM booking_food_draft_items WHERE booking_id = b.booking_id
        -- Deleted drafts still show that a missing food order is not evidence of no food.
    ) d ON TRUE
    WHERE b.legacy_snapshot_status IS NULL
      AND COALESCE(b.seat_subtotal_gross, 0) = 0
      AND COALESCE(b.food_subtotal_gross, 0) = 0
      AND COALESCE(b.rank_discount_amount, 0) = 0
      AND COALESCE(b.voucher_discount_seat, 0) = 0
      AND COALESCE(b.voucher_discount_food, 0) = 0
      AND COALESCE(b.net_food_amount, 0) = 0
      AND b.net_seat_amount = b.total_price
      -- Positive modern snapshots, including free bookings with gross prices,
      -- cannot match this fingerprint. Counter/contact snapshots are also modern.
      AND b.booking_source = 'ONLINE' AND b.created_by_staff_id IS NULL
      AND b.contact_name IS NULL AND b.contact_email IS NULL AND b.contact_phone IS NULL
), validated AS (
    SELECT c.*,
           CASE
               WHEN ticket_count = 0 THEN 'MISSING_TICKET_EVIDENCE'
               WHEN bad_tickets > 0 THEN 'INVALID_TICKET_EVIDENCE'
               WHEN order_count > 1 OR bad_orders > 0 THEN 'INCONSISTENT_FOOD_EVIDENCE'
               WHEN order_count = 0 AND (food_draft_count > 0 OR food_fulfillment_status <> 'NOT_APPLICABLE')
                   THEN 'MISSING_FOOD_EVIDENCE'
               WHEN discount_amount IS NULL OR late_discount_amount IS NULL
                   OR discount_amount < late_discount_amount THEN 'INVALID_DISCOUNT_EVIDENCE'
               WHEN late_discount_amount > 0 AND (discount_amount <> late_discount_amount OR voucher_id IS NOT NULL)
                   THEN 'CONFLICTING_DISCOUNTS'
               WHEN discount_amount > late_discount_amount AND voucher_id IS NULL THEN 'MISSING_VOUCHER_EVIDENCE'
           END AS evidence_error
    FROM candidates c
), possible AS (
    SELECT v.booking_id, r.factor,
           round(v.seat_gross * r.factor - v.late_discount_amount - allocation.voucher_seat, 2) AS net_seat,
           allocation.voucher_seat,
           v.discount_amount - v.late_discount_amount - allocation.voucher_seat AS voucher_food
    FROM validated v
    CROSS JOIN (VALUES (1.00::numeric), (0.98::numeric), (0.97::numeric), (0.95::numeric)) r(factor)
    CROSS JOIN LATERAL (
        SELECT CASE WHEN v.seat_gross + v.food_gross = 0 THEN 0::numeric
                    ELSE round((v.discount_amount - v.late_discount_amount) * v.seat_gross
                               / (v.seat_gross + v.food_gross), 2) END AS voucher_seat
    ) allocation
    WHERE v.evidence_error IS NULL
      AND v.discount_amount <= (v.seat_gross + v.food_gross) * r.factor
      AND round((v.seat_gross + v.food_gross) * r.factor - v.discount_amount, 2) = v.total_price
      AND (v.late_discount_amount = 0 OR round(v.seat_gross * r.factor * 0.50, 2) = v.late_discount_amount)
      -- With zero paid total, a larger discount could have hidden any lower rank.
      -- Only an entirely undiscounted free booking is unambiguous without more evidence.
      AND (v.total_price > 0 OR v.discount_amount = 0)
), solutions AS (
    SELECT p.booking_id, count(DISTINCT (p.net_seat, p.voucher_seat, p.voucher_food)) AS split_count,
           min(p.net_seat) AS net_seat, min(p.voucher_seat) AS voucher_seat, min(p.voucher_food) AS voucher_food
    FROM possible p
    GROUP BY p.booking_id
)
SELECT v.booking_id, v.seat_gross, v.food_gross, s.net_seat,
       v.total_price - s.net_seat AS net_food,
       v.seat_gross + v.food_gross - v.total_price - v.discount_amount AS rank_discount,
       s.voucher_seat, s.voucher_food,
       COALESCE(v.evidence_error,
           CASE WHEN COALESCE(s.split_count, 0) = 0 THEN 'UNRECONCILED_TOTAL_OR_DISCOUNT'
                WHEN s.split_count > 1 THEN 'AMBIGUOUS_ROUNDING'
                WHEN s.net_seat < 0 OR s.net_seat > v.total_price
                    OR s.voucher_seat < 0 OR s.voucher_food < 0
                    OR v.seat_gross + v.food_gross < v.total_price + v.discount_amount
                    THEN 'INVALID_NET_SPLIT'
           END) AS review_reason,
       jsonb_build_object('migration', '20261019_06', 'total_price', v.total_price,
           'discount_amount', v.discount_amount, 'late_discount_amount', v.late_discount_amount,
           'ticket_count', v.ticket_count, 'seat_gross', v.seat_gross,
           'food_order_count', v.order_count, 'food_gross', v.food_gross,
           'food_draft_count', v.food_draft_count, 'matching_splits', COALESCE(s.split_count, 0),
           'previous_net_seat', v.net_seat_amount, 'previous_net_food', v.net_food_amount) AS evidence
FROM validated v LEFT JOIN solutions s ON s.booking_id = v.booking_id;

UPDATE bookings b
SET seat_subtotal_gross = r.seat_gross,
    food_subtotal_gross = r.food_gross,
    rank_discount_amount = r.rank_discount,
    voucher_discount_seat = r.voucher_seat,
    voucher_discount_food = r.voucher_food,
    net_seat_amount = r.net_seat,
    net_food_amount = r.net_food,
    legacy_snapshot_status = 'RECOVERED',
    legacy_snapshot_reason = 'RECONCILED_HISTORICAL_PRICES_AND_DISCOUNTS',
    legacy_snapshot_evidence = r.evidence
FROM legacy_snapshot_repair r
WHERE b.booking_id = r.booking_id AND r.review_reason IS NULL;

-- Leave financial values untouched when evidence is insufficient. The marker is
-- mandatory input to the application guard, not a zero-value refund instruction.
UPDATE bookings b
SET legacy_snapshot_status = 'REVIEW_REQUIRED',
    legacy_snapshot_reason = r.review_reason,
    legacy_snapshot_evidence = r.evidence
FROM legacy_snapshot_repair r
WHERE b.booking_id = r.booking_id AND r.review_reason IS NOT NULL;

COMMIT;
