package com.uit.backend_cinema.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Util HMAC chung dùng cho các cổng thanh toán (MoMo, VNPay, ZaloPay, ...).
 */
public class HmacUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HMAC_SHA512 = "HmacSHA512";

    private HmacUtil() {}

    /**
     * Ký dữ liệu bằng thuật toán HMAC-SHA256.
     *
     * @param data      raw data string (fields đã sort alphabetical & join bằng &)
     * @param secretKey secret key của gateway
     * @return hex string lowercase
     */
    public static String signHmacSHA256(String data, String secretKey) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHexString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Lỗi khi ký HMAC-SHA256", e);
        }
    }

    /**
     * Xác thực chữ ký HMAC-SHA256.
     *
     * @param data              raw data string
     * @param secretKey         secret key
     * @param expectedSignature chữ ký cần kiểm tra
     * @return true nếu chữ ký hợp lệ
     */
    public static boolean verifyHmacSHA256(String data, String secretKey, String expectedSignature) {
        if (expectedSignature == null) return false;
        String actualSignature = signHmacSHA256(data, secretKey);
        return actualSignature.equals(expectedSignature);
    }

    /**
     * Ký dữ liệu bằng thuật toán HMAC-SHA512.
     */
    public static String signHmacSHA512(String data, String secretKey) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            Mac mac = Mac.getInstance(HMAC_SHA512);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHexString(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Lỗi khi ký HMAC-SHA512", e);
        }
    }

    /**
     * Xác thực chữ ký HMAC-SHA512.
     */
    public static boolean verifyHmacSHA512(String data, String secretKey, String expectedSignature) {
        if (expectedSignature == null) return false;
        String actualSignature = signHmacSHA512(data, secretKey);
        return actualSignature.equals(expectedSignature);
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
