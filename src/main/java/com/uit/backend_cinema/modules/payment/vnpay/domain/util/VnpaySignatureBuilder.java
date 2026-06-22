package com.uit.backend_cinema.modules.payment.vnpay.domain.util;

import com.uit.backend_cinema.common.util.HmacUtil;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Utility để build payment URL và xác thực chữ ký (SecureHash) của VNPay.
 * Đảm bảo tuân thủ kiến trúc Clean Architecture (không phụ thuộc vào Spring config/infra).
 */
public class VnpaySignatureBuilder {

    private VnpaySignatureBuilder() {}

    /**
     * Build URL thanh toán hoàn chỉnh cho VNPay.
     * 
     * @param params map chứa các tham số vnp_*
     * @param payUrl base URL thanh toán (ví dụ sandbox vpcpay.html)
     * @param hashSecret khóa bí mật để ký
     * @return URL redirect hoàn chỉnh
     */
    public static String buildPaymentUrl(Map<String, String> params, String payUrl, String hashSecret) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        // Remove trailing & if it was added by a trailing empty parameter
        if (queryUrl.endsWith("&")) {
            queryUrl = queryUrl.substring(0, queryUrl.length() - 1);
        }
        
        String rawHash = hashData.toString();
        if (rawHash.endsWith("&")) {
            rawHash = rawHash.substring(0, rawHash.length() - 1);
        }

        String secureHash = HmacUtil.signHmacSHA512(rawHash, hashSecret);
        
        return payUrl + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Xác thực chữ ký từ các tham số nhận được trong IPN/Return callback.
     * 
     * @param params các tham số nhận được từ callback (đã được Spring decode)
     * @param hashSecret khóa bí mật để verify
     * @return true nếu chữ ký hợp lệ
     */
    public static boolean verifySignature(Map<String, String> params, String hashSecret) {
        String expectedHash = params.get("vnp_SecureHash");
        if (expectedHash == null || expectedHash.isEmpty()) {
            return false;
        }

        Map<String, String> fields = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            if (fieldValue != null && fieldValue.length() > 0) {
                String encodedName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII);
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII);
                fields.put(encodedName, encodedValue);
            }
        }

        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            hashData.append(fieldName);
            hashData.append('=');
            hashData.append(fieldValue);
            if (itr.hasNext()) {
                hashData.append('&');
            }
        }

        return HmacUtil.verifyHmacSHA512(hashData.toString(), hashSecret, expectedHash);
    }
}
