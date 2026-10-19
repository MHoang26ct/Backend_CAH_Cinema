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
public class VnpayRefundRequestDTO {

    @JsonProperty("vnp_RequestId")
    private String requestId;

    @JsonProperty("vnp_Version")
    @Builder.Default
    private String version = "2.1.0";

    @JsonProperty("vnp_Command")
    @Builder.Default
    private String command = "refund";

    @JsonProperty("vnp_TmnCode")
    private String tmnCode;

    /**
     * 02: Hoàn trả toàn phần
     * 03: Hoàn trả một phần
     */
    @JsonProperty("vnp_TransactionType")
    private String transactionType;

    @JsonProperty("vnp_TxnRef")
    private String txnRef;

    @JsonProperty("vnp_Amount")
    private Long amount;

    @JsonProperty("vnp_OrderInfo")
    private String orderInfo;

    @JsonProperty("vnp_TransactionNo")
    private String transactionNo;

    /**
     * Thời gian ghi nhận thanh toán gốc (vnp_CreateDate gốc của đơn thanh toán), format yyyyMMddHHmmss
     */
    @JsonProperty("vnp_TransactionDate")
    private String transactionDate;

    @JsonProperty("vnp_CreateBy")
    private String createBy;

    @JsonProperty("vnp_CreateDate")
    private String createDate;

    @JsonProperty("vnp_IpAddr")
    private String ipAddr;

    @JsonProperty("vnp_SecureHash")
    private String secureHash;
}
