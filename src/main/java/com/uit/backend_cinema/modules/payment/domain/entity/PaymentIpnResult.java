package com.uit.backend_cinema.modules.payment.domain.entity;

import lombok.*;

/**
 * Kết quả sau khi verify & parse IPN callback từ bất kỳ gateway nào.
 * Chung cho MoMo, VNPay, ZaloPay, ...
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIpnResult {
    /** IPN hợp lệ và thanh toán thành công? */
    private boolean success;
    /** orderId đã gửi lên gateway */
    private String orderId;
    /** Mã giao dịch phía gateway */
    private String transactionId;
    /** Loại thanh toán: qr, app, webApp, ... */
    private String payType;
    /** Message từ gateway */
    private String message;
    /** Result code gốc từ gateway */
    private int resultCode;
}
