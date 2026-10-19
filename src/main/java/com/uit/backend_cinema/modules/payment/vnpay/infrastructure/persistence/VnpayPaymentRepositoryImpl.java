package com.uit.backend_cinema.modules.payment.vnpay.infrastructure.persistence;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequestStatus;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentIpnResult;
import com.uit.backend_cinema.modules.payment.vnpay.config.VnpayProperties;
import com.uit.backend_cinema.modules.payment.vnpay.domain.repository.VnpayPaymentRepository;
import com.uit.backend_cinema.modules.payment.vnpay.domain.util.VnpaySignatureBuilder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Infrastructure implementation cho VnpayPaymentRepository.
 */
@Repository
public class VnpayPaymentRepositoryImpl implements VnpayPaymentRepository {

    private final VnpayProperties vnpayProperties;
    private static final java.time.ZoneId VN_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(VN_ZONE);

    public VnpayPaymentRepositoryImpl(VnpayProperties vnpayProperties) {
        this.vnpayProperties = vnpayProperties;
    }

    @Override
    public PaymentGatewayRequest createVnpayPayment(PaymentGatewayRequest request, String ipAddr, String bankCode) {
        Map<String, String> vnpParams = new HashMap<>();
        
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayProperties.getTmnCode());
        
        // VNPay amount format: nhân với 100 để khử thập phân
        long vnpAmount = request.getAmount() * 100;
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        
        // Mã đối tác
        vnpParams.put("vnp_TxnRef", request.getOrderId());
        vnpParams.put("vnp_OrderInfo", request.getOrderInfo());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddr);

        // Ngày tạo và ngày hết hạn
        String vnpCreateDate = request.getVnpCreateDate();
        if (vnpCreateDate == null || vnpCreateDate.isBlank()) {
            LocalDateTime createTime = LocalDateTime.now(VN_ZONE);
            vnpCreateDate = createTime.format(DATE_FORMATTER);
            request.setVnpCreateDate(vnpCreateDate);
        }
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        String vnpExpireDate = request.getVnpExpireDate();
        if (vnpExpireDate == null || vnpExpireDate.isBlank()) {
            LocalDateTime expireTime = LocalDateTime.now(VN_ZONE).plusMinutes(vnpayProperties.getPaymentRequestTtlMinutes());
            vnpExpireDate = expireTime.format(DATE_FORMATTER);
            request.setVnpExpireDate(vnpExpireDate);
        }
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // Mã ngân hàng (nếu có chọn trước)
        if (bankCode != null && !bankCode.trim().isEmpty()) {
            vnpParams.put("vnp_BankCode", bankCode);
        }

        try {
            // Build redirect URL chứa signature
            String payUrl = VnpaySignatureBuilder.buildPaymentUrl(
                    vnpParams, 
                    vnpayProperties.getPayUrl(), 
                    vnpayProperties.getHashSecret()
            );

            // VNPay không gọi API bằng HTTP Client, chỉ build URL redirect cho FE
            request.setPayUrl(payUrl);
            request.setResultCode(0); // Thành công build URL
            request.setResponseMessage("Build URL thanh toán VNPay thành công");
            request.setStatus(PaymentGatewayRequestStatus.CREATED);
        } catch (Exception e) {
            request.setResultCode(-1);
            request.setResponseMessage("Build URL thất bại: " + e.getMessage());
            request.setStatus(PaymentGatewayRequestStatus.FAILED);
        }

        return request;
    }

    @Override
    public PaymentGatewayRequest createPayment(PaymentGatewayRequest request) {
        throw new UnsupportedOperationException("Sử dụng createVnpayPayment thay thế vì VNPay yêu cầu IP của client");
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaymentIpnResult handleIpn(Object rawIpnRequest) {
        if (!(rawIpnRequest instanceof Map)) {
            throw new IllegalArgumentException("VNPay IPN request must be a Map of query parameters");
        }

        Map<String, String> params = (Map<String, String>) rawIpnRequest;

        // 1. Xác thực chữ ký
        boolean isValidSignature = VnpaySignatureBuilder.verifySignature(params, vnpayProperties.getHashSecret());
        if (!isValidSignature) {
            return null;
        }

        // 2. Parse thông tin thanh toán
        String orderId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String cardType = params.get("vnp_CardType");

        String payDate = params.get("vnp_PayDate");
        String bankTranNo = params.get("vnp_BankTranNo");
        String amountStr = params.get("vnp_Amount");
        Long amount = null;
        try {
            if (amountStr != null) {
                amount = Long.parseLong(amountStr);
            }
        } catch (NumberFormatException ignored) {}

        // Giao dịch thành công khi cả hai mã đều là "00"
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        int resultCode = -1;
        try {
            if (responseCode != null) {
                resultCode = Integer.parseInt(responseCode);
            }
        } catch (NumberFormatException e) {
            // Keep -1
        }

        return PaymentIpnResult.builder()
                .success(success)
                .orderId(orderId)
                .transactionId(transactionNo)
                .payType(cardType != null ? cardType : bankCode)
                .amount(amount)
                .payDate(payDate)
                .transactionStatus(transactionStatus)
                .bankCode(bankCode)
                .bankTranNo(bankTranNo)
                .cardType(cardType)
                .message("VNPay response: Code=" + responseCode + ", Status=" + transactionStatus)
                .resultCode(resultCode)
                .build();
    }
}
