package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.seat.api.util.SeatLabelUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SeatLabelUtilTest {

    @Test
    @DisplayName("1. row 1.0 -> 'A'")
    void toRowLabelA() {
        assertEquals("A", SeatLabelUtil.toRowLabel(new BigDecimal("1.0")));
    }

    @Test
    @DisplayName("2. row 2.0 -> 'B'")
    void toRowLabelB() {
        assertEquals("B", SeatLabelUtil.toRowLabel(new BigDecimal("2.0")));
    }

    @Test
    @DisplayName("3. row 26.0 -> 'Z'")
    void toRowLabelZ() {
        assertEquals("Z", SeatLabelUtil.toRowLabel(new BigDecimal("26.0")));
    }

    @Test
    @DisplayName("4. row 27.0 -> 'AA'")
    void toRowLabelAA() {
        assertEquals("AA", SeatLabelUtil.toRowLabel(new BigDecimal("27.0")));
    }

    @Test
    @DisplayName("5. row 28.0 -> 'AB'")
    void toRowLabelAB() {
        assertEquals("AB", SeatLabelUtil.toRowLabel(new BigDecimal("28.0")));
    }

    @Test
    @DisplayName("6. row 1.5 -> null (aisle)")
    void toRowLabelAisle() {
        assertNull(SeatLabelUtil.toRowLabel(new BigDecimal("1.5")));
    }

    @Test
    @DisplayName("7. row null -> null")
    void toRowLabelNull() {
        assertNull(SeatLabelUtil.toRowLabel(null));
    }

    @Test
    @DisplayName("8. col 5.0 -> '5'")
    void toColLabelFive() {
        assertEquals("5", SeatLabelUtil.toColLabel(new BigDecimal("5.0")));
    }

    @Test
    @DisplayName("9. col 12.0 -> '12'")
    void toColLabelTwelve() {
        assertEquals("12", SeatLabelUtil.toColLabel(new BigDecimal("12.0")));
    }

    @Test
    @DisplayName("10. col 3.5 -> null (aisle)")
    void toColLabelAisle() {
        assertNull(SeatLabelUtil.toColLabel(new BigDecimal("3.5")));
    }

    @Test
    @DisplayName("11. col null -> null")
    void toColLabelNull() {
        assertNull(SeatLabelUtil.toColLabel(null));
    }
}
