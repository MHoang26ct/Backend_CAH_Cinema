package com.uit.backend_cinema.modules.booking.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingRequestDTO {
    private String quoteToken;
    private BigDecimal expectedRefundAmount;
    private String reason;
}
