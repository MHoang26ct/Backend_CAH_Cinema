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
public class BusinessOverview {
    private LocalDate from;
    private LocalDate to;
    private BigDecimal totalRevenue;
    private BigDecimal ticketRevenue;
    private BigDecimal foodRevenue;
    private Long totalTicketsSold;
    private Long totalBookingsPaid;
    private BigDecimal totalDiscount;
    private BigDecimal averageOrderValue;
}
