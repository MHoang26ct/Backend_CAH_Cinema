package com.uit.backend_cinema.modules.payment.vnpay.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayQueryRequestDTO {

    @JsonProperty("vnp_RequestId")
    private String requestId;

    @JsonProperty("vnp_Version")
    @Builder.Default
    private String version = "2.1.0";

    @JsonProperty("vnp_Command")
    @Builder.Default
    private String command = "querydr";

    @JsonProperty("vnp_TmnCode")
    private String tmnCode;

    @JsonProperty("vnp_TxnRef")
    private String txnRef;

    @JsonProperty("vnp_OrderInfo")
    private String orderInfo;

    @JsonProperty("vnp_TransactionDate")
    private String transactionDate;

    @JsonProperty("vnp_CreateDate")
    private String createDate;

    @JsonProperty("vnp_IpAddr")
    private String ipAddr;

    @JsonProperty("vnp_SecureHash")
    private String secureHash;
}
