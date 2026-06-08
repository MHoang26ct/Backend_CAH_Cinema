package com.uit.backend_cinema.modules.payment.momo.domain.repository;

import com.uit.backend_cinema.modules.payment.domain.repository.PaymentGatewayRepository;

/**
 * Repository riêng cho MoMo — kế thừa contract chung.
 * Có thể thêm method MoMo-specific nếu cần.
 */
public interface MomoPaymentRepository extends PaymentGatewayRepository {
    // Kế thừa:
    // PaymentGatewayRequest createPayment(PaymentGatewayRequest request);
    // PaymentIpnResult handleIpn(Object rawIpnRequest);
}
