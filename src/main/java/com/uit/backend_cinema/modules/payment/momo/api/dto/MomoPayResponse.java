package com.uit.backend_cinema.modules.payment.momo.api.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response trả về cho client sau khi tạo đơn MoMo thành công.
 */
@Getter
@Builder
public class MomoPayResponse {

    /** URL redirect sang trang thanh toán MoMo (dùng cho web) */
    private String payUrl;

    /** URL mở thẳng app MoMo (dùng cho mobile) */
    private String deeplink;

    /** Dữ liệu để render mã QR (không phải URL ảnh, cần dùng thư viện QR) */
    private String qrCodeUrl;

    /** orderId đã gửi lên MoMo (= bookingId) */
    private String momoOrderId;
}
