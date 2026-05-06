package com.uit.backend_cinema.modules.booking.api.dto;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderItemRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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
