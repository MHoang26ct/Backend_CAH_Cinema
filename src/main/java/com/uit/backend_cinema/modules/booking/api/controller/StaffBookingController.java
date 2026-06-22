package com.uit.backend_cinema.modules.booking.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;

@RestController
@RequestMapping("/api/v1/staff/bookings")
public class StaffBookingController {
    private final BookingService bookingService;

    public StaffBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/{bookingId}/confirm-payment")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> confirmPayment(
            @PathVariable Long bookingId,
            @Valid @RequestBody ConfirmPaymentRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ConfirmPaymentResponseDTO response = bookingService.confirmPayment(user.getUserId(), bookingId, requestDTO);
        return ResponseEntity.ok(ApiResponse.success(response, "Xác nhận thanh toán thành công"));
    }
}
