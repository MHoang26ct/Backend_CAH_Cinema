package com.uit.backend_cinema.modules.payment.infrastructure.entity;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA entity map bảng payment_requests.
 * Dùng chung cho mọi cổng thanh toán (MoMo, VNPay, ZaloPay, ...).
 */
@Entity
@Table(name = "payment_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PaymentGatewayRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_request_id")
    private Long paymentRequestId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "gateway", nullable = false, length = 30)
    private String gateway;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    /**
     * UUID do server sinh khi thực sự gọi MoMo API.
     * Tách biệt với requestId của client để tránh MoMo reject trùng requestId.
     */
    @Column(name = "gateway_request_id", length = 100)
    private String gatewayRequestId;

    @Column(name = "order_id", nullable = false, length = 200)
    private String orderId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "order_info", length = 255)
    private String orderInfo;

    @Column(name = "pay_url", columnDefinition = "TEXT")
    private String payUrl;

    @Column(name = "deeplink", columnDefinition = "TEXT")
    private String deeplink;

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "result_code")
    private Integer resultCode;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "gateway_trans_id", length = 100)
    private String gatewayTransId;

    @Column(name = "pay_type", length = 30)
    private String payType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentGatewayRequestStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
