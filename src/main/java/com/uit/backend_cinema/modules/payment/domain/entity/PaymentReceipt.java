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
public class PaymentReceipt {
    private Long paymentReceiptId;
    private Long bookingId;
    private Long paymentRequestId;
    private String gateway;
    private String transactionReference;
    private String orderId;
    private BigDecimal amount;
    private LocalDateTime payDate;
    private LocalDateTime verifiedAt;
    private PaymentReceiptStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
