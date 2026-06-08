package com.uit.backend_cinema.modules.payment.momo.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequestStatus;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentIpnResult;
import com.uit.backend_cinema.modules.payment.momo.api.dto.MomoIpnRequest;
import com.uit.backend_cinema.modules.payment.momo.config.MomoProperties;
import com.uit.backend_cinema.modules.payment.momo.domain.repository.MomoPaymentRepository;
import com.uit.backend_cinema.modules.payment.momo.domain.util.MomoSignatureBuilder;
import com.uit.backend_cinema.modules.payment.momo.infrastructure.client.MomoApiClient;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

@Repository
public class MomoPaymentRepositoryImpl implements MomoPaymentRepository {

    private final MomoApiClient momoApiClient;
    private final MomoProperties momoProperties;

    public MomoPaymentRepositoryImpl(MomoApiClient momoApiClient, MomoProperties momoProperties) {
        this.momoApiClient = momoApiClient;
        this.momoProperties = momoProperties;
    }

    @Override
    public PaymentGatewayRequest createPayment(PaymentGatewayRequest request) {
        String reqType = request.getRequestType() != null && !request.getRequestType().isBlank()
                ? request.getRequestType()
                : "captureWallet";

        // 1. Build signature (dùng MomoSignatureBuilder)
        String signature = MomoSignatureBuilder.signCreateRequest(
                momoProperties.getAccessKey(),
                String.valueOf(request.getAmount()),
                "",
                momoProperties.getIpnUrl(),
                request.getOrderId(),
                request.getOrderInfo(),
                momoProperties.getPartnerCode(),
                momoProperties.getRedirectUrl(),
                request.getGatewayRequestId(),
                reqType,
                momoProperties.getSecretKey()
        );

        // 2. Build MoMo-specific request body
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("partnerCode", momoProperties.getPartnerCode());
        requestBody.put("requestId", request.getGatewayRequestId());
        requestBody.put("amount", request.getAmount());
        requestBody.put("orderId", request.getOrderId());
        requestBody.put("orderInfo", request.getOrderInfo());
        requestBody.put("redirectUrl", momoProperties.getRedirectUrl());
        requestBody.put("ipnUrl", momoProperties.getIpnUrl());
        requestBody.put("requestType", reqType);
        requestBody.put("extraData", "");
        requestBody.put("autoCapture", true);
        requestBody.put("lang", "vi");
        
        if ("payWithCC".equals(reqType)) {
            Map<String, String> userInfo = new LinkedHashMap<>();
            userInfo.put("email", request.getCustomerEmail());
            requestBody.put("userInfo", userInfo);
        }

        requestBody.put("signature", signature);

        // 3. Gọi MoMo API
        JsonNode response = momoApiClient.createPaymentOrder(requestBody);

        // 4. Cập nhật request với kết quả
        request.setPayUrl(response.path("payUrl").asText(null));
        request.setQrCodeUrl(response.path("qrCodeUrl").asText(null));
        request.setDeeplink(response.path("deeplink").asText(null));
        request.setResultCode(response.path("resultCode").asInt(-1));
        request.setResponseMessage(response.path("message").asText(null));
        request.setStatus(request.getResultCode() == 0
                ? PaymentGatewayRequestStatus.CREATED
                : PaymentGatewayRequestStatus.FAILED);

        return request;
    }

    @Override
    public PaymentIpnResult handleIpn(Object rawIpnRequest) {
        MomoIpnRequest ipn = (MomoIpnRequest) rawIpnRequest;

        // 1. Verify signature
        boolean valid = MomoSignatureBuilder.verifyIpnSignature(
                ipn, momoProperties.getAccessKey(), momoProperties.getSecretKey());
        if (!valid) return null;

        // 2. Parse thành domain object chung
        return PaymentIpnResult.builder()
                .success(ipn.getResultCode() == 0)
                .orderId(ipn.getOrderId())
                .transactionId(ipn.getTransId() != null ? ipn.getTransId().toString() : null)
                .payType(ipn.getPayType())
                .message(ipn.getMessage())
                .resultCode(ipn.getResultCode())
                .build();
    }
}
