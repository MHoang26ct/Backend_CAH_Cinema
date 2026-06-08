package com.uit.backend_cinema.modules.payment.domain.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Domain entity chung cho payment request với bất kỳ gateway nào.
 * Map bảng payment_requests trong DB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayRequest {

    private Long paymentRequestId;

    /** ID booking trong hệ thống */
    private Long bookingId;

    /** Tên cổng thanh toán: MOMO, VNPAY, ZALOPAY, ... */
    private String gateway;

    /** Idempotency key do client tạo (UUID) */
    private String requestId;

    /** UUID do server sinh khi thực sự gọi MoMo API (Idempotency server -> MoMo) */
    private String gatewayRequestId;

    /** Mã đơn hàng gửi lên gateway (= bookingId.toString()) */
    private String orderId;

    /** Số tiền thanh toán (VND) */
    private Long amount;

    /** Mô tả đơn hàng */
    private String orderInfo;

    // ---- Response từ gateway khi tạo đơn ----

    /** URL redirect sang trang thanh toán của gateway */
    private String payUrl;

    /** URL deep link mở thẳng app gateway */
    private String deeplink;

    /** Dữ liệu để render mã QR */
    private String qrCodeUrl;

    /** Result code từ gateway khi tạo đơn (0 = thành công) */
    private Integer resultCode;

    /** Message từ gateway */
    private String responseMessage;

    // ---- Cập nhật từ IPN / callback ----

    /** Mã giao dịch phía gateway (transId của MoMo, ...) */
    private String gatewayTransId;

    /** Hình thức thanh toán: qr, app, webApp, ... */
    private String payType;

    /** Trạng thái nội bộ */
    private PaymentGatewayRequestStatus status;

    /** Loại thanh toán MoMo (captureWallet, payWithATM, payWithCC). Không lưu xuống DB */
    private String requestType;

    /** Email khách hàng. Dùng cho payWithCC. Không lưu xuống DB */
    private String customerEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
