package com.uit.backend_cinema.modules.report.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaRevenue {
    private Long cinemaId;
    private String cinemaName;
    private BigDecimal ticketRevenue;
    private Long ticketsSold;
    private Long bookingCount;
}
