package com.uit.backend_cinema.modules.invoice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    private Long invoiceId;
    private Long bookingId;
    private String paymentMethod;
    private BigDecimal amountPaid;
    private LocalDateTime createdAt;
}
