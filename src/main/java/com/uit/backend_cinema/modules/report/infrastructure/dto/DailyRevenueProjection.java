package com.uit.backend_cinema.modules.report.infrastructure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRevenueProjection {
    LocalDate getReportDate();
    BigDecimal getRevenue();
    Long getBookingCount();
    Long getTicketCount();
}
