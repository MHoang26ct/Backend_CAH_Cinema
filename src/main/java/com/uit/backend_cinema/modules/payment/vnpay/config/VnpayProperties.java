package com.uit.backend_cinema.modules.payment.vnpay.config;

import com.uit.backend_cinema.modules.payment.vnpay.domain.config.VnpayPaymentConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnpayProperties implements VnpayPaymentConfig {
    private String tmnCode;
    private String hashSecret;
    private String payUrl;
    private String returnUrl;
    private String ipnUrl;
    private int paymentRequestTtlMinutes = 15;
}
