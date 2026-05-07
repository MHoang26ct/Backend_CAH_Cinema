package com.uit.backend_cinema.modules.booking.api.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderItemRequestDTO;
import lombok.Data;

@Data
public class CreateBookingRequestDTO {
    @NotNull(message = "Showtime ID không được để trống")
    private Long showtimeId;

    private Long voucherId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private BookingPaymentMethod paymentMethod;

    @NotEmpty(message = "Danh sách ghế không được trống")
    private List<Long> seatIds;

    @Valid
    private List<FoodOrderItemRequestDTO> foodItems = new ArrayList<>();
}
