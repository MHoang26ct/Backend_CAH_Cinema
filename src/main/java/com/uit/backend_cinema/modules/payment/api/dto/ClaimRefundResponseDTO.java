package com.uit.backend_cinema.modules.payment.api.dto;

import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRefundResponseDTO {
    private Long refundId;
    private Long bookingId;
    private RefundStatus status;
    private Long actorStaffId;
    private LocalDateTime claimedAt;
    private BigDecimal amount;
    private String message;
}
