# PR #49 — cancellation/refund review fixes

## Behaviour changes

1. Admin showtime cancellation locks the showtimes and active bookings, cancels pending bookings even before their deadline, and expires drafts/releases their voucher holds. Redis seat locks are released only after SQL commit (TTL remains the fallback on Redis failure). A later successful provider collection creates a refund obligation, never new tickets for the cancelled booking.
2. Showtime state, booking cancellation and refund obligations commit together. A positive refund requires a verified receipt matching the successful payment confirmation's gateway and reference, as well as the booking's paid amount. Missing historical evidence or a failed insert aborts cancellation rather than reporting `SUCCEEDED`. Do not manufacture a receipt just to bypass this guard.
3. VNPay claims are row-locked and committed before network dispatch. `PROCESSING` and `UNKNOWN` are revisited; after `vnpay.refund-review-after-seconds` (default 900, minimum 60), unresolved cases become `REVIEW_REQUIRED`. This is escalation, **not** automated proof of payout. The integration has no safe per-refund query contract: `querydr` of the original payment does not establish refund success. Never resend an uncertain refund automatically. HTTP automatic retries/follow-ups for refund requests are disabled; authenticated successful responses must match refund type and amount. Outcome/membership reconciliation is committed together.
4. `/cancel` requires a current `quoteToken` or `expectedRefundAmount`; when both are supplied both must match. The token is a quote fingerprint, not an authorization credential. Ownership/staff authorization still applies.
5. Legacy net seat/food snapshots are repaired by an additive migration. Ambiguous historical evidence is quarantined, not silently treated as all-seat money.
6. Extra/late collections cannot be selected for a normal cancellation refund simply because their receipt appears first.

## Migration and deployment order

**This change does not deploy itself. Do not edit or rerun historical migrations on an existing database.**

- Back up the database; stop application writers/workers for a maintenance window.
- Confirm incremental migrations through `20261018_05_refund_membership_adjustment.sql` have been applied.
- Apply `migrations/20261019_06_legacy_food_snapshots.sql` once. It is transactional and rerunnable, uses a five-second lock timeout, and does not overwrite modern snapshots or already classified rows.
- Inspect `legacy_snapshot_status`, `legacy_snapshot_reason` and `legacy_snapshot_evidence`. Only historical persisted ticket/food prices, discount totals and documented historical rank rules are used; current catalog prices, voucher values and member rank are not evidence.
- `RECOVERED` means a unique net split was reconstructed under the documented allocation policy. `REVIEW_REQUIRED` leaves the original financial fields unchanged and blocks customer quote/cancel. Null means unclassified/modern, not proof of recovery.
- Audit any **existing** customer refunds attached to affected legacy bookings separately. This migration does not rewrite refund amounts, prior policy snapshots, or completed payouts. Already-paid under/over-refunds require manual reconciliation; do not resend them.
- Deploy the matching application version before reopening cancellation. It maps `legacy_snapshot_status`; starting it before migration can fail schema validation/querying. Do not roll back to code without the quarantine guard while allowing customer cancellation.

Useful read-only inspection:

```sql
SELECT legacy_snapshot_status, legacy_snapshot_reason, count(*)
FROM bookings
WHERE legacy_snapshot_status IS NOT NULL
GROUP BY legacy_snapshot_status, legacy_snapshot_reason;

SELECT r.refund_id, r.booking_id, r.status, r.amount,
       b.legacy_snapshot_status, b.legacy_snapshot_reason
FROM refunds r JOIN bookings b ON b.booking_id = r.booking_id
WHERE b.legacy_snapshot_status IS NOT NULL
  AND r.cause = 'CUSTOMER_CANCELLED';
```

Manual resolution requires authenticated provider evidence (including the specific refund identity/amount), an audit record and idempotent membership reconciliation. `REVIEW_REQUIRED` is not permission to mark success or pay again. No new manual settlement endpoint is introduced by this patch.

## Regression coverage

- Pending cancellation before expiry, staff lock ownership, duplicate cancellation, and late Stripe/VNPay success without ticket resurrection.
- Real PostgreSQL/Spring transaction rollback of schedule and earlier pending-booking changes when a paid booking lacks evidence; Redis locks are not released on rollback.
- Funding receipt selection across multiple collections; missing evidence/failed refund insert; mandatory quote confirmation and stale quote after food fulfillment.
- VNPay concurrent workers, durable claim, processing/unknown timeouts, disabled client, crash/legacy timestamps, no blind retries/querydr, signed response type/amount and HTTP retry safety. Concurrency in `VnpayRefundRecoveryTest` uses a simulated transactional lock, not a provider.
- Real PostgreSQL legacy migration: 80%/50% ticket refunds plus full unfulfilled-food refunds, fulfilled food excluded, rank/voucher/late-discount allocation, rounding, missing/contradictory evidence, modern snapshots unchanged and safe reruns.

All automated provider interactions are mocks/local HTTP fixtures. No live charge, server migration or real-provider refund is part of this verification.
