package com.uit.backend_cinema.modules.payment.infrastructure.repository;

import com.uit.backend_cinema.modules.payment.infrastructure.entity.PaymentGatewayRequestJpaEntity;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaPaymentGatewayRequestRepository
        extends JpaRepository<PaymentGatewayRequestJpaEntity, Long> {

    /** Tìm payment request theo bookingId — dùng để kiểm tra đã tạo đơn chưa */
    Optional<PaymentGatewayRequestJpaEntity> findFirstByBookingIdOrderByCreatedAtDesc(Long bookingId);

    /** Tìm payment request theo gateway và bookingId */
    Optional<PaymentGatewayRequestJpaEntity> findFirstByGatewayAndBookingIdOrderByCreatedAtDesc(String gateway, Long bookingId);

    /** Tìm các request CREATED cần đối soát */
    java.util.List<PaymentGatewayRequestJpaEntity> findTop10ByGatewayAndStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            String gateway, PaymentGatewayRequestStatus status, java.time.LocalDateTime threshold);

    /** Tìm theo gateway + orderId — dùng khi nhận IPN callback */
    Optional<PaymentGatewayRequestJpaEntity> findByGatewayAndOrderId(String gateway, String orderId);
}
