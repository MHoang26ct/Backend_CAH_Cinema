package com.uit.backend_cinema.unit_test;

import java.math.BigDecimal;

import com.uit.backend_cinema.common.util.VndMoney;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class VndMoneyTest {

    @ParameterizedTest
    @CsvSource({
            "100.4, 100",
            "100.5, 101",
            "100.6, 101",
            "0.0, 0",
            "99999.49, 99999",
            "99999.50, 100000"
    })
    void testRoundHalfUp(String input, String expected) {
        BigDecimal result = VndMoney.round(new BigDecimal(input));
        assertEquals(new BigDecimal(expected), result);
        assertEquals(0, result.scale());
    }

    @Test
    void testRequireWholeSucceedsForWholeAmounts() {
        BigDecimal whole = new BigDecimal("50000");
        assertSame(whole, VndMoney.requireWhole(whole));
    }

    @Test
    void testRequireWholeFailsForFractionalAmounts() {
        assertThrows(ArithmeticException.class, () -> VndMoney.requireWhole(new BigDecimal("50000.5")));
    }

    @Test
    void testToGatewayAmountMultipliesBy100() {
        assertEquals(10000000L, VndMoney.toGatewayAmount(new BigDecimal("100000")));
        assertEquals(0L, VndMoney.toGatewayAmount(BigDecimal.ZERO));
    }

    @Test
    void testFromGatewayAmountDividesBy100() {
        assertEquals(new BigDecimal("100000"), VndMoney.fromGatewayAmount(10000000L));
        assertEquals(BigDecimal.ZERO, VndMoney.fromGatewayAmount(0L));
    }

    @Test
    void testNegativeAmountsRejected() {
        assertThrows(IllegalArgumentException.class, () -> VndMoney.round(new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> VndMoney.requireWhole(new BigDecimal("-10")));
        assertThrows(IllegalArgumentException.class, () -> VndMoney.toGatewayAmount(new BigDecimal("-500")));
        assertThrows(IllegalArgumentException.class, () -> VndMoney.fromGatewayAmount(-100L));
    }
}
