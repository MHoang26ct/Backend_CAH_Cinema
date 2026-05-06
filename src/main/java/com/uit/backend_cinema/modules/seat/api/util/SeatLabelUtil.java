package com.uit.backend_cinema.modules.seat.api.util;

import java.math.BigDecimal;

/**
 * Tiện ích chuyển đổi tọa độ số sang nhãn hiển thị cho Seat Map.
 *
 * Quy ước:
 *   - Tọa độ nguyên (x.0): Là ghế thật → trả về nhãn
 *   - Tọa độ lẻ   (x.5): Là đường đi  → trả về null
 *
 * Ví dụ:
 *   row 1.0 → "A", row 2.0 → "B", ..., row 27.0 → "AA"
 *   row 1.5 → null
 *   col 5.0 → "5", col 5.5 → null
 */
public class SeatLabelUtil {
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private SeatLabelUtil() {}

    /**
     * Chuyển tọa độ hàng (số) sang ký tự chữ cái.
     * 1.0 → "A", 2.0 → "B", ..., 26.0 → "Z", 27.0 → "AA"
     * x.5 → null (đường đi ngang)
     */
    public static String toRowLabel(BigDecimal row) {
        if (row == null) return null;
        if (isAisle(row)) return null;

        int n = row.intValue(); // 1-based
        StringBuilder label = new StringBuilder();
        while (n > 0) {
            n--; // chuyển về 0-based
            label.insert(0, (char) ('A' + (n % 26)));
            n /= 26;
        }
        return label.toString();
    }

    /**
     * Chuyển tọa độ cột (số) sang chuỗi hiển thị.
     * 1.0 → "1", 2.0 → "2"...
     * x.5 → null (đường đi dọc)
     */
    public static String toColLabel(BigDecimal col) {
        if (col == null) return null;
        if (isAisle(col)) return null;
        return String.valueOf(col.intValue());
    }

    /**
     * Kiểm tra tọa độ có phải đường đi không.
     * Đường đi được đánh dấu bằng giá trị .5 (VD: 4.5, 6.5)
     */
    private static boolean isAisle(BigDecimal value) {
        // Nhân 2 rồi kiểm tra lẻ: 4.5 * 2 = 9 (lẻ) → aisle
        return value.multiply(TWO).remainder(TWO)
                .compareTo(BigDecimal.ZERO) != 0;
    }
}
