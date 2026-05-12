package com.uit.backend_cinema.modules.auth.domain.entity;

/**
 * Hạng thành viên dựa trên total_points tích lũy.
 * Điểm được tính từ tổng tiền thanh toán: 40,000 VNĐ = 1 điểm.
 * <ul>
 *   <li>SILVER  : 0 – 500 điểm</li>
 *   <li>GOLD    : 501 – 1500 điểm</li>
 *   <li>DIAMOND : > 1500 điểm</li>
 * </ul>
 */
public enum UserRank {
    SILVER,
    GOLD,
    DIAMOND;

    private static final int GOLD_THRESHOLD    = 501;
    private static final int DIAMOND_THRESHOLD = 1501;

    public static UserRank fromPoints(int totalPoints) {
        if (totalPoints >= DIAMOND_THRESHOLD) return DIAMOND;
        if (totalPoints >= GOLD_THRESHOLD)    return GOLD;
        return SILVER;
    }
}
