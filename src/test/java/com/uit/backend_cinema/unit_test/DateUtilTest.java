package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.util.DateUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {
    @Test
    @DisplayName("FMT01: Ngày giờ thông thường được định dạng dd/MM/yyyy HH:mm:ss")
    void formatsDateTime() {
        assertEquals("12/10/2026 09:05:07",
                DateUtil.format(LocalDateTime.of(2026, 10, 12, 9, 5, 7)));
    }

    @Test
    @DisplayName("FMT02: null trả về null")
    void formatsNull() {
        assertNull(DateUtil.format(null));
    }

    @Test
    @DisplayName("FMT03: Ngày nhuận lúc nửa đêm được thêm số 0")
    void formatsLeapDayAtMidnight() {
        assertEquals("29/02/2024 00:00:00",
                DateUtil.format(LocalDateTime.of(2024, 2, 29, 0, 0)));
    }

    @Test
    @DisplayName("FMT04: Giây cuối năm giữ đúng ngày và giờ")
    void formatsLastSecondOfYear() {
        assertEquals("31/12/2026 23:59:59",
                DateUtil.format(LocalDateTime.of(2026, 12, 31, 23, 59, 59)));
    }

    @ParameterizedTest(name = "{0}: {1} -> weekend={2}")
    @CsvSource({
            "WE01, 2026-10-12, false",
            "WE02, 2026-10-13, false",
            "WE03, 2026-10-14, false",
            "WE04, 2026-10-15, false",
            "WE05, 2026-10-16, false",
            "WE06, 2026-10-17, true",
            "WE07, 2026-10-18, true"
    })
    void identifiesWeekend(String caseId, LocalDate date, boolean expected) {
        assertEquals(expected, DateUtil.isWeekend(date));
    }

    @Test
    @DisplayName("WE08: null gây NullPointerException theo hợp đồng hiện tại")
    void rejectsNullDate() {
        assertThrows(NullPointerException.class, () -> DateUtil.isWeekend(null));
    }
}
