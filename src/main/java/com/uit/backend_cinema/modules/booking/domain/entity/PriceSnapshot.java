package com.uit.backend_cinema.modules.booking.domain.entity;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

/**
 * Snapshot of the price breakdown at the moment a booking's amount is finalised.
 * Immutable record used for persistence, cancellation‐quote and audit.
 *
 * Rounding rule already applied: every BigDecimal here is a whole VND amount.
 *
 * <pre>
 * seatSubtotalGross       — sum of seat prices × showtime/seat‐type multipliers
 * foodSubtotalGross       — sum of (unit price × qty) for food draft items
 * rankDiscount            — (seatSubtotalGross + foodSubtotalGross) × rank rate
 * seatSubtotalAfterRank   — seatSubtotalGross − proportional rank share (seat)
 * foodSubtotalAfterRank   — foodSubtotalGross − proportional rank share (food)
 * lateDiscount            — 50 % of seatSubtotalAfterRank when applicable
 * voucherDiscountTotal    — voucher value applied to (seat+food after rank − late)
 * voucherDiscountSeat     — voucher split: seat share, rounded
 * voucherDiscountFood     — voucher split: food share = voucherDiscountTotal − voucherDiscountSeat
 * netSeat                 — seatSubtotalAfterRank − lateDiscount − voucherDiscountSeat
 * netFood                 — foodSubtotalAfterRank − voucherDiscountFood
 * totalAmount             — netSeat + netFood (≥ 0)
 * </pre>
 *
 * Invariant: netSeat + netFood = totalAmount.
 */
@Value
@Builder
public class PriceSnapshot {
    BigDecimal seatSubtotalGross;
    BigDecimal foodSubtotalGross;
    BigDecimal rankDiscount;
    BigDecimal seatSubtotalAfterRank;
    BigDecimal foodSubtotalAfterRank;
    BigDecimal lateDiscount;
    BigDecimal voucherDiscountTotal;
    BigDecimal voucherDiscountSeat;
    BigDecimal voucherDiscountFood;
    BigDecimal netSeat;
    BigDecimal netFood;
    BigDecimal totalAmount;
}
