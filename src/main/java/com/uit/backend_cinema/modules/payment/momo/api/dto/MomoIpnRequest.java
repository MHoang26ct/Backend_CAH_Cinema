package com.uit.backend_cinema.modules.payment.momo.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Body IPN callback từ MoMo (POST tới ipnUrl).
 * MoMo gửi kết quả giao dịch sau khi user thanh toán xong.
 * Tài liệu: https://developers.momo.vn/#/docs/en/aiov2/?id=payment-notification
 */
@Getter
@Setter
public class MomoIpnRequest {

    @JsonProperty("partnerCode")
    private String partnerCode;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("orderInfo")
    private String orderInfo;

    @JsonProperty("orderType")
    private String orderType;

    /** Mã giao dịch phía MoMo */
    @JsonProperty("transId")
    private Long transId;

    /** 0 = thành công, khác 0 = thất bại */
    @JsonProperty("resultCode")
    private Integer resultCode;

    @JsonProperty("message")
    private String message;

    /** webApp | app | qr | miniapp */
    @JsonProperty("payType")
    private String payType;

    @JsonProperty("responseTime")
    private Long responseTime;

    @JsonProperty("extraData")
    private String extraData;

    /** Chữ ký HMAC-SHA256 từ MoMo — cần verify trước khi xử lý */
    @JsonProperty("signature")
    private String signature;
}
