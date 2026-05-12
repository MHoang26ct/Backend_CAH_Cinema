package com.uit.backend_cinema.modules.auth.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * Hóa đơn đầy đủ của 1 booking — dùng cho màn hình lịch sử giao dịch / invoice.
 */
@Getter
@Builder
public class FullInvoiceDTO {

    // Booking
    private Long bookingId;
    private String bookingStatus;
    private String paymentMethod;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;
    private LocalDateTime bookingCreatedAt;

    // Voucher (nullable)
    private String voucherCode;

    // Showtime
    private Long showtimeId;
    private String movieFormat;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Movie
    private Long movieId;
    private String movieTitle;
    private String moviePosterUrl;

    // Cinema / Room
    private String cinemaName;
    private String roomName;

    // Ghế được đặt
    private List<TicketDTO> seats;

    // Thức ăn kèm
    private List<FoodLineDTO> foods;

    // Food order total
    private BigDecimal foodTotalPrice;

    // Inner DTOs
    @Getter
    @Builder
    public static class TicketDTO {
        private Long seatId;
        private BigDecimal seatRow;
        private BigDecimal seatCol;
        private String seatType;
        private BigDecimal ticketPrice;
    }

    @Getter
    @Builder
    public static class FoodLineDTO {
        private Long foodId;
        private String foodName;
        private String foodImageUrl;
        private String foodCategory;
        private int quantity;
        private BigDecimal unitPrice;
    }
}
