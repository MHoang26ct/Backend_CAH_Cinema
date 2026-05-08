package com.uit.backend_cinema.modules.report.infrastructure.repository.dto;

import java.math.BigDecimal;

public interface MovieRevenueProjection {
    Long getMovieId();
    String getMovieTitle();
    BigDecimal getTicketRevenue();
    Long getTicketsSold();
    Long getBookingCount();
}
