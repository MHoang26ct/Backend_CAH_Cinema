package com.uit.backend_cinema.modules.payment.momo.domain.config;

public interface MomoPaymentConfig {
    String getPartnerCode();
    String getAccessKey();
    String getSecretKey();
    String getIpnUrl();
    String getRedirectUrl();
    int getPaymentRequestTtlMinutes();
}
