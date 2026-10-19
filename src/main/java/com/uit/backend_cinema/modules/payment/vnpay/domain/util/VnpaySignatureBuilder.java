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

    /**
     * Build hash data cho querydr request theo VNPay 2.1.0:
     * vnp_RequestId|vnp_Version|vnp_Command|vnp_TmnCode|vnp_TxnRef|vnp_TransactionDate|vnp_CreateDate|vnp_IpAddr|vnp_OrderInfo
     */
    public static String buildQuerydrRequestHash(String requestId, String version, String command,
                                                 String tmnCode, String txnRef, String transactionDate,
                                                 String createDate, String ipAddr, String orderInfo,
                                                 String hashSecret) {
        String rawHash = String.join("|",
                emptyIfNull(requestId),
                emptyIfNull(version),
                emptyIfNull(command),
                emptyIfNull(tmnCode),
                emptyIfNull(txnRef),
                emptyIfNull(transactionDate),
                emptyIfNull(createDate),
                emptyIfNull(ipAddr),
                emptyIfNull(orderInfo)
        );
        return HmacUtil.signHmacSHA512(rawHash, hashSecret);
    }

    /**
     * Build hash data cho refund request theo VNPay 2.1.0:
     * vnp_RequestId|vnp_Version|vnp_Command|vnp_TmnCode|vnp_TransactionType|vnp_TxnRef|vnp_Amount|vnp_TransactionNo|vnp_TransactionDate|vnp_CreateBy|vnp_CreateDate|vnp_IpAddr|vnp_OrderInfo
     */
    public static String buildRefundRequestHash(String requestId, String version, String command,
                                                String tmnCode, String transactionType, String txnRef,
                                                long amount, String transactionNo, String transactionDate,
                                                String createBy, String createDate, String ipAddr,
                                                String orderInfo, String hashSecret) {
        String rawHash = String.join("|",
                emptyIfNull(requestId),
                emptyIfNull(version),
                emptyIfNull(command),
                emptyIfNull(tmnCode),
                emptyIfNull(transactionType),
                emptyIfNull(txnRef),
                String.valueOf(amount),
                emptyIfNull(transactionNo),
                emptyIfNull(transactionDate),
                emptyIfNull(createBy),
                emptyIfNull(createDate),
                emptyIfNull(ipAddr),
                emptyIfNull(orderInfo)
        );
        return HmacUtil.signHmacSHA512(rawHash, hashSecret);
    }

    /**
     * Build hash data cho querydr / refund response theo VNPay 2.1.0:
     * vnp_ResponseId|vnp_Command|vnp_ResponseCode|vnp_Message|vnp_TmnCode|vnp_TxnRef|vnp_Amount|vnp_BankCode|vnp_PayDate|vnp_TransactionNo|vnp_TransactionType|vnp_TransactionStatus|vnp_OrderInfo
     */
    public static String buildTransactionResponseHash(String responseId, String command, String responseCode,
                                                      String message, String tmnCode, String txnRef,
                                                      Object amount, String bankCode, String payDate,
                                                      String transactionNo, String transactionType,
                                                      String transactionStatus, String orderInfo,
                                                      String hashSecret) {
        String rawHash = String.join("|",
                emptyIfNull(responseId),
                emptyIfNull(command),
                emptyIfNull(responseCode),
                emptyIfNull(message),
                emptyIfNull(tmnCode),
                emptyIfNull(txnRef),
                amount != null ? String.valueOf(amount) : "",
                emptyIfNull(bankCode),
                emptyIfNull(payDate),
                emptyIfNull(transactionNo),
                emptyIfNull(transactionType),
                emptyIfNull(transactionStatus),
                emptyIfNull(orderInfo)
        );
        return HmacUtil.signHmacSHA512(rawHash, hashSecret);
    }

    /**
     * Xác thực chữ ký phản hồi giao dịch (querydr / refund) từ VNPay.
     */
    public static boolean verifyTransactionResponse(String secureHash, String responseId, String command,
                                                    String responseCode, String message, String tmnCode,
                                                    String txnRef, Object amount, String bankCode,
                                                    String payDate, String transactionNo, String transactionType,
                                                    String transactionStatus, String orderInfo,
                                                    String hashSecret) {
        if (secureHash == null || secureHash.isBlank() || hashSecret == null || hashSecret.isBlank()) {
            return false;
        }
        String calculated = buildTransactionResponseHash(responseId, command, responseCode, message,
                tmnCode, txnRef, amount, bankCode, payDate, transactionNo, transactionType, transactionStatus,
                orderInfo, hashSecret);
        return calculated.equalsIgnoreCase(secureHash.trim());
    }

    private static String emptyIfNull(String val) {
        return val != null ? val : "";
    }
}
