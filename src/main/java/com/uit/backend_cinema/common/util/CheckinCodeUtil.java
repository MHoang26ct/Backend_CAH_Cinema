package com.uit.backend_cinema.common.util;

import java.security.SecureRandom;

public final class CheckinCodeUtil {
    // Bảng chữ cái loại bỏ 0, O, 1, I để chống nhầm lẫn khi đọc hoặc gõ thủ công
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CheckinCodeUtil() {}

    /**
     * Sinh mã vé rút gọn định dạng TK-XXXX-XXXX (14 ký tự).
     * Rất ngắn gọn, tính ngẫu nhiên cao (32^8 ~ 1.1 nghìn tỷ khả năng),
     * khi tạo mã QR chỉ cần QR Version 1 (21x21 ô), quét cực nhanh.
     */
    public static String generateCode() {
        StringBuilder sb = new StringBuilder("TK-");
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
