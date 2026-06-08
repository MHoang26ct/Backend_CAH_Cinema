package com.uit.backend_cinema.modules.payment.momo.domain.util;

import com.uit.backend_cinema.common.util.HmacUtil;
import com.uit.backend_cinema.modules.payment.momo.api.dto.MomoIpnRequest;

/**
 * Build raw data string theo format MoMo (alphabetical order) và ký HMAC-SHA256.
 * Tài liệu: https://developers.momo.vn/#/docs/en/aiov2/?id=payment-method
 */
public class MomoSignatureBuilder {

    private MomoSignatureBuilder() {}

    /**
     * Tạo chữ ký cho request tạo đơn MoMo (/create).
     * Raw data: accessKey&amount&extraData&ipnUrl&orderId&orderInfo&partnerCode&redirectUrl&requestId&requestType
     */
    public static String signCreateRequest(
            String accessKey,
            String amount,
            String extraData,
            String ipnUrl,
            String orderId,
            String orderInfo,
            String partnerCode,
            String redirectUrl,
            String requestId,
            String requestType,
            String secretKey) {

        String rawData = "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        return HmacUtil.signHmacSHA256(rawData, secretKey);
    }

    /**
     * Xác thực chữ ký IPN callback từ MoMo.
     * Raw data: accessKey&amount&extraData&message&orderId&orderInfo&orderType
     *           &partnerCode&payType&requestId&responseTime&resultCode&transId
     */
    public static boolean verifyIpnSignature(MomoIpnRequest ipn, String accessKey, String secretKey) {
        String rawData = "accessKey=" + accessKey
                + "&amount=" + ipn.getAmount()
                + "&extraData=" + nullToEmpty(ipn.getExtraData())
                + "&message=" + nullToEmpty(ipn.getMessage())
                + "&orderId=" + ipn.getOrderId()
                + "&orderInfo=" + nullToEmpty(ipn.getOrderInfo())
                + "&orderType=" + nullToEmpty(ipn.getOrderType())
                + "&partnerCode=" + ipn.getPartnerCode()
                + "&payType=" + nullToEmpty(ipn.getPayType())
                + "&requestId=" + ipn.getRequestId()
                + "&responseTime=" + ipn.getResponseTime()
                + "&resultCode=" + ipn.getResultCode()
                + "&transId=" + ipn.getTransId();

        return HmacUtil.verifyHmacSHA256(rawData, secretKey, ipn.getSignature());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
