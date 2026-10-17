package com.uit.backend_cinema.modules.payment.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {
    private Long refundId;
    private Long paymentReceiptId;
    private Long bookingId;
    private RefundCause cause;
    private BigDecimal amount;
    private String policySnapshot;
    private RefundStatus status;
    private String requestReference;
    private Long actorStaffId;
    private LocalDateTime claimedAt;
    private LocalDateTime processedAt;
    private String gatewayResponseCode;
    private String gatewayTransactionNo;
    private String gatewayResponseMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
