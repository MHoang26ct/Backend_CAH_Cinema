package com.uit.backend_cinema.modules.payment.infrastructure.repository;

import com.uit.backend_cinema.modules.payment.infrastructure.entity.PaymentGatewayRequestJpaEntity;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaPaymentGatewayRequestRepository
        extends JpaRepository<PaymentGatewayRequestJpaEntity, Long> {

    /** Tìm payment request theo bookingId — dùng để kiểm tra đã tạo đơn chưa */
    Optional<PaymentGatewayRequestJpaEntity> findFirstByBookingIdOrderByCreatedAtDesc(Long bookingId);

    /** Tìm theo gateway + orderId — dùng khi nhận IPN callback */
    Optional<PaymentGatewayRequestJpaEntity> findByGatewayAndOrderId(String gateway, String orderId);
}
