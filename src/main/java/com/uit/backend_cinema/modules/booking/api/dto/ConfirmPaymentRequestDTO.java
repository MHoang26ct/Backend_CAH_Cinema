package com.uit.backend_cinema.modules.booking.api.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmPaymentRequestDTO {
    @NotBlank(message = "Mã tham chiếu thanh toán không được để trống")
    private String paymentRef;

    @NotBlank(message = "Cổng thanh toán không được để trống")
    private String gateway;
}
