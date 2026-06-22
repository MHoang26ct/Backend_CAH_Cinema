package com.uit.backend_cinema.modules.payment.domain.repository;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentIpnResult;

/**
 * Contract chung cho mọi payment gateway (MoMo, VNPay, ZaloPay, ...).
 * Mỗi submodule phải extends interface này.
 */
public interface PaymentGatewayRepository {

    /**
     * Gọi API gateway bên ngoài để tạo đơn thanh toán.
     *
     * @param request PaymentGatewayRequest chứa orderId, amount, orderInfo, ...
     * @return PaymentGatewayRequest đã cập nhật kết quả: payUrl, qrCodeUrl, deeplink, resultCode, status
     */
    PaymentGatewayRequest createPayment(PaymentGatewayRequest request);

    /**
     * Verify signature & parse IPN callback từ gateway.
     *
     * @param rawIpnRequest dữ liệu IPN gốc từ gateway (mỗi gateway có format riêng)
     * @return PaymentIpnResult chứa kết quả đã parse, hoặc null nếu signature không hợp lệ
     */
    PaymentIpnResult handleIpn(Object rawIpnRequest);
}
