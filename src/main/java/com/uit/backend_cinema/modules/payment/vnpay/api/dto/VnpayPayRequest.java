package com.uit.backend_cinema.modules.payment.vnpay.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body khi client gọi POST /api/v1/bookings/{bookingId}/vnpay/pay.
 */
@Getter
@Setter
public class VnpayPayRequest {

    /**
     * Idempotency key do client tạo (UUID).
     * Dùng để kiểm tra tránh gửi trùng yêu cầu cho cùng một booking.
     */
    @NotBlank(message = "requestId không được để trống")
    private String requestId;
}
