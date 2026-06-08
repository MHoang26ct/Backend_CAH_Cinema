package com.uit.backend_cinema.modules.payment.vnpay.domain.config;

public interface VnpayPaymentConfig {
    String getTmnCode();
    String getHashSecret();
    String getPayUrl();
    String getReturnUrl();
    String getIpnUrl();
    int getPaymentRequestTtlMinutes();
}
