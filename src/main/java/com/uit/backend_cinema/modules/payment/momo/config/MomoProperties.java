package com.uit.backend_cinema.modules.payment.momo.config;

import com.uit.backend_cinema.modules.payment.momo.domain.config.MomoPaymentConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình MoMo, đọc từ application.yml prefix "momo".
 */
@Configuration
@ConfigurationProperties(prefix = "momo")
@Getter
@Setter
public class MomoProperties implements MomoPaymentConfig {

    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String redirectUrl;
    private String ipnUrl;

    /**
     * Thời gian (phút) tối đa một payment request CREATED được coi là còn hợp lệ.
     * Nếu quá thời gian này mà chưa nhận IPN, QR được coi là đã hết hạn và cho phép tạo đơn mới.
     * Default: 10 phút.
     */
    private int paymentRequestTtlMinutes = 10;

    public String getCreateUrl() {
        return endpoint + "/create";
    }

    public String getQueryUrl() {
        return endpoint + "/query";
    }
}
