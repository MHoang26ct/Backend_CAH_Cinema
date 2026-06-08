package com.uit.backend_cinema.modules.payment.vnpay.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response JSON trả về cho VNPay khi nhận IPN callback.
 * Phải khớp chính xác key "RspCode" và "Message".
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayIpnResponse {

    @JsonProperty("RspCode")
    private String rspCode;

    @JsonProperty("Message")
    private String message;
}
