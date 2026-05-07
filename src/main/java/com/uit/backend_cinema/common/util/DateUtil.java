package com.uit.backend_cinema.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private static final String DEFAULT_DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    private DateUtil() {
        throw new IllegalAccessError("Utility class");
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT);
        return dateTime.format(formatter);
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().equals(DayOfWeek.SATURDAY) || date.getDayOfWeek().equals(DayOfWeek.SUNDAY);
    }
}
//
//JwtAuthenticationFilter (để chặn các request không có token) hay GlobalExceptionHandler (để bắt lỗi tập trung bằng ApiResponse) trước?
