package com.uit.backend_cinema.modules.payment.momo.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body khi client gọi POST /api/v1/bookings/{bookingId}/momo/pay.
 */
@Getter
@Setter
public class MomoPayRequest {

    /**
     * Idempotency key do client tạo (UUID).
     * Dùng làm requestId gửi lên MoMo — đảm bảo không tạo đơn trùng.
     */
    @NotBlank(message = "requestId không được để trống")
    private String requestId;

    /**
     * Loại hình thanh toán MoMo: captureWallet, payWithATM, payWithCC.
     * Mặc định là captureWallet.
     */
    private String requestType = "captureWallet";
}
