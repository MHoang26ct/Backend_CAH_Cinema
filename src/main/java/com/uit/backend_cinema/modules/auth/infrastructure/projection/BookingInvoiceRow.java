package com.uit.backend_cinema.modules.auth.infrastructure.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Spring Data projection — ánh xạ từng row flat từ native query JOIN
 * (1 row = 1 ghế × 1 food item hoặc null nếu không có food).
 * Được assemble thành {@code FullInvoiceDTO} trong UseCase.
 */
public interface BookingInvoiceRow {

    // Booking
    Long getBookingId();
    String getBookingStatus();
    String getPaymentMethod();
    BigDecimal getDiscountAmount();
    BigDecimal getTotalPrice();
    LocalDateTime getBookingCreatedAt();

    // Voucher (nullable)
    String getVoucherCode();

    // Showtime
    Long getShowtimeId();
    String getMovieFormat();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();

    // Movie
    Long getMovieId();
    String getMovieTitle();
    String getMoviePosterUrl();

    // Cinema / Room
    String getCinemaName();
    String getRoomName();

    // Seat + Ticket
    Long getSeatId();
    BigDecimal getSeatRow();
    BigDecimal getSeatCol();
    String getSeatType();
    BigDecimal getTicketPrice();

    // Food (nullable — không phải booking nào cũng có food)
    Long getFoodId();
    String getFoodName();
    String getFoodImageUrl();
    String getFoodCategory();
    Integer getFoodQuantity();
    BigDecimal getFoodUnitPrice();

    // Food order total (nullable)
    BigDecimal getFoodTotalPrice();
}
