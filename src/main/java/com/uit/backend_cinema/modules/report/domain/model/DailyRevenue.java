package com.uit.backend_cinema.modules.report.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenue {
    private LocalDate reportDate;
    private BigDecimal revenue;
    private Long bookingCount;
    private Long ticketCount;
}
