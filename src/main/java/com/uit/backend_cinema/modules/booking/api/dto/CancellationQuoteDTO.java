package com.uit.backend_cinema.modules.booking.api.dto;

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
public class CancellationQuoteDTO {
    private Long bookingId;
    private LocalDateTime showtimeStartTime;
    private LocalDateTime quoteTime;
    private double hoursUntilShowtime;
    private BigDecimal ticketRefundRate;
    private BigDecimal netSeatAmount;
    private BigDecimal ticketRefundAmount;
    private BigDecimal netFoodAmount;
    private BigDecimal foodRefundAmount;
    private BigDecimal totalRefundAmount;
    private BigDecimal retainedAmount;
    private boolean canCancel;
    private String reason;
    private String quoteToken;
}
