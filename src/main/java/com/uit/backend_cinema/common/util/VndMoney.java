package com.uit.backend_cinema.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact VND arithmetic. Rounding belongs at the pricing boundary, never at the gateway. */
public final class VndMoney {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private VndMoney() {
    }

    public static BigDecimal round(BigDecimal amount) {
        requireNonNegative(amount);
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal requireWhole(BigDecimal amount) {
        requireNonNegative(amount);
        return amount.setScale(0, RoundingMode.UNNECESSARY);
    }

    public static long toGatewayAmount(BigDecimal amount) {
        return requireWhole(amount).multiply(HUNDRED).longValueExact();
    }

    public static BigDecimal fromGatewayAmount(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Gateway amount must not be negative");
        return BigDecimal.valueOf(amount).divide(HUNDRED, 0, RoundingMode.UNNECESSARY);
    }

    public static void requireNonNegative(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must be non-null and non-negative");
        }
    }
}
