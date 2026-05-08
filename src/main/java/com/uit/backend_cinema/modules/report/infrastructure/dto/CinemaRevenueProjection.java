package com.uit.backend_cinema.modules.report.infrastructure.dto;

import java.math.BigDecimal;

public interface CinemaRevenueProjection {
    Long getCinemaId();
    String getCinemaName();
    BigDecimal getTicketRevenue();
    Long getTicketsSold();
    Long getBookingCount();
}
