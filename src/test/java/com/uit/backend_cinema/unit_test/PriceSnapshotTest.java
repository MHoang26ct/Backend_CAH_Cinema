package com.uit.backend_cinema.unit_test;

import java.math.BigDecimal;

import com.uit.backend_cinema.modules.booking.domain.entity.PriceSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceSnapshotTest {

    @Test
    void testPriceSnapshotHoldsExactWholeVndAmounts() {
        PriceSnapshot snapshot = PriceSnapshot.builder()
                .seatSubtotalGross(new BigDecimal("200000"))
                .foodSubtotalGross(new BigDecimal("100000"))
                .rankDiscount(new BigDecimal("15000"))
                .seatSubtotalAfterRank(new BigDecimal("190000"))
                .foodSubtotalAfterRank(new BigDecimal("95000"))
                .lateDiscount(BigDecimal.ZERO)
                .voucherDiscountTotal(new BigDecimal("50000"))
                .voucherDiscountSeat(new BigDecimal("33333"))
                .voucherDiscountFood(new BigDecimal("16667"))
                .netSeat(new BigDecimal("156667"))
                .netFood(new BigDecimal("78333"))
                .totalAmount(new BigDecimal("235000"))
                .build();

        assertEquals(new BigDecimal("235000"), snapshot.getTotalAmount());
        assertEquals(snapshot.getTotalAmount(), snapshot.getNetSeat().add(snapshot.getNetFood()));
        assertEquals(snapshot.getVoucherDiscountTotal(), snapshot.getVoucherDiscountSeat().add(snapshot.getVoucherDiscountFood()));
    }
}
