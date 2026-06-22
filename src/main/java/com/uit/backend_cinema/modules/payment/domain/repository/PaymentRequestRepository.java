package com.uit.backend_cinema.modules.payment.domain.repository;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import java.util.Optional;

/**
 * Repository interface cho persistence payment request (save/find).
 * Dùng bởi PaymentRequestService — dùng chung cho mọi gateway.
 */
public interface PaymentRequestRepository {
    PaymentGatewayRequest save(PaymentGatewayRequest request);
    Optional<PaymentGatewayRequest> findLatestByBookingId(Long bookingId);
    Optional<PaymentGatewayRequest> findByGatewayAndOrderId(String gateway, String orderId);
}
