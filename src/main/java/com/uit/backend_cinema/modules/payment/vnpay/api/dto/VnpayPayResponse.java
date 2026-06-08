package com.uit.backend_cinema.modules.payment.vnpay.api.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response trả về cho client sau khi tạo đơn VNPay thành công.
 */
@Getter
@Builder
public class VnpayPayResponse {

    /** URL redirect sang trang thanh toán VNPay */
    private String payUrl;

    /** orderId đã gửi lên VNPay (vnp_TxnRef) */
    private String vnpayOrderId;
}
