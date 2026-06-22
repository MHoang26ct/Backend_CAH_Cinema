package com.uit.backend_cinema.modules.payment.infrastructure.mapper;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.infrastructure.entity.PaymentGatewayRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentInfraMapper {

    public PaymentGatewayRequest toDomain(PaymentGatewayRequestJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return PaymentGatewayRequest.builder()
                .paymentRequestId(entity.getPaymentRequestId())
                .bookingId(entity.getBookingId())
                .gateway(entity.getGateway())
                .requestId(entity.getRequestId())
                .gatewayRequestId(entity.getGatewayRequestId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .orderInfo(entity.getOrderInfo())
                .payUrl(entity.getPayUrl())
                .deeplink(entity.getDeeplink())
                .qrCodeUrl(entity.getQrCodeUrl())
                .resultCode(entity.getResultCode())
                .responseMessage(entity.getResponseMessage())
                .gatewayTransId(entity.getGatewayTransId())
                .payType(entity.getPayType())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PaymentGatewayRequestJpaEntity toJpaEntity(PaymentGatewayRequest domain) {
        if (domain == null) {
            return null;
        }
        PaymentGatewayRequestJpaEntity entity = new PaymentGatewayRequestJpaEntity();
        entity.setPaymentRequestId(domain.getPaymentRequestId());
        entity.setBookingId(domain.getBookingId());
        entity.setGateway(domain.getGateway());
        entity.setRequestId(domain.getRequestId());
        entity.setGatewayRequestId(domain.getGatewayRequestId());
        entity.setOrderId(domain.getOrderId());
        entity.setAmount(domain.getAmount());
        entity.setOrderInfo(domain.getOrderInfo());
        entity.setPayUrl(domain.getPayUrl());
        entity.setDeeplink(domain.getDeeplink());
        entity.setQrCodeUrl(domain.getQrCodeUrl());
        entity.setResultCode(domain.getResultCode());
        entity.setResponseMessage(domain.getResponseMessage());
        entity.setGatewayTransId(domain.getGatewayTransId());
        entity.setPayType(domain.getPayType());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
