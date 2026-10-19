package com.uit.backend_cinema.modules.payment.vnpay.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VnpayRefundResponseDTO {

    @JsonProperty("vnp_ResponseId")
    private String responseId;

    @JsonProperty("vnp_Command")
    private String command;

    @JsonProperty("vnp_ResponseCode")
    private String responseCode;

    @JsonProperty("vnp_Message")
    private String message;

    @JsonProperty("vnp_TmnCode")
    private String tmnCode;

    @JsonProperty("vnp_TxnRef")
    private String txnRef;

    @JsonProperty("vnp_Amount")
    private Long amount;

    @JsonProperty("vnp_OrderInfo")
    private String orderInfo;

    @JsonProperty("vnp_BankCode")
    private String bankCode;

    @JsonProperty("vnp_PayDate")
    private String payDate;

    @JsonProperty("vnp_TransactionNo")
    private String transactionNo;

    @JsonProperty("vnp_TransactionType")
    private String transactionType;

    @JsonProperty("vnp_TransactionStatus")
    private String transactionStatus;

    @JsonProperty("vnp_SecureHash")
    private String secureHash;

    /**
     * Theo tài liệu VNPay 2.1.0:
     * - ResponseCode = "00": Yêu cầu hoàn được tiếp nhận xử lý
     * - TransactionStatus:
     *     "00": Thành công
     *     "05": VNPAY đang xử lý
     *     "06": VNPAY đã gửi yêu cầu hoàn tiền sang Ngân hàng
     *     "09": Bị từ chối
     * - ResponseCode "94": Yêu cầu đã được gửi trước đó và đang xử lý.
     */
    public boolean isRefundSucceeded() {
        return "00".equals(responseCode) && "00".equals(transactionStatus);
    }

    public boolean isRefundProcessing() {
        return "94".equals(responseCode)
                || ("00".equals(responseCode) && ("05".equals(transactionStatus) || "06".equals(transactionStatus)));
    }
}
