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
public class MovieRevenue {
    private Long movieId;
    private String movieTitle;
    private BigDecimal ticketRevenue;
    private Long ticketsSold;
    private Long bookingCount;
}
