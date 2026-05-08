package com.uit.backend_cinema.modules.report.infrastructure.repository.dto;

import java.math.BigDecimal;

public interface OverviewProjection {
    BigDecimal getTotalRevenue();
    BigDecimal getTicketRevenue();
    BigDecimal getFoodRevenue();
    Long getTotalTicketsSold();
    Long getTotalBookingsPaid();
    BigDecimal getTotalDiscount();
}
