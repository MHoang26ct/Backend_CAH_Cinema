package com.uit.backend_cinema.modules.booking.api.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.booking.api.dto.ConfirmPaymentRequestDTO;
import com.uit.backend_cinema.modules.booking.api.dto.ConfirmPaymentResponseDTO;
import com.uit.backend_cinema.modules.booking.api.dto.CreateBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.api.mapper.BookingApiMapper;
import com.uit.backend_cinema.modules.booking.domain.entity.PrePaymentBookingQuote;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final BookingApiMapper bookingApiMapper;

    public BookingController(BookingService bookingService, BookingApiMapper bookingApiMapper) {
        this.bookingService = bookingService;
        this.bookingApiMapper = bookingApiMapper;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody CreateBookingRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(user.getUserId(), requestDTO);
        var response = bookingApiMapper.toResponse(quote);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo booking chờ thanh toán thành công"));
    }

    @PostMapping("/{bookingId}/confirm-payment")
    public ResponseEntity<?> confirmPayment(
            @PathVariable Long bookingId,
            @Valid @RequestBody ConfirmPaymentRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ConfirmPaymentResponseDTO response = bookingService.confirmPayment(user.getUserId(), bookingId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(response, "Xác nhận thanh toán thành công"));
    }
}
