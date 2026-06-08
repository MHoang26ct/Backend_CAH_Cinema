package com.uit.backend_cinema.modules.payment.domain.entity;

/**
 * Trạng thái nội bộ của một payment request với gateway.
 */
public enum PaymentGatewayRequestStatus {
    /** Đã gọi gateway tạo đơn thành công, chờ user thanh toán */
    CREATED,

    /** IPN/callback xác nhận thanh toán thành công */
    PAID,

    /** IPN/callback xác nhận thanh toán thất bại */
    FAILED
}
