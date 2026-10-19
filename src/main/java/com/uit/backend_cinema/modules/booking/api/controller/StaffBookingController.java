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

import com.uit.backend_cinema.modules.booking.api.dto.CreateBookingResponseDTO;
import com.uit.backend_cinema.modules.booking.api.dto.CreateStaffBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.api.dto.CustomerLookupResponseDTO;
import com.uit.backend_cinema.modules.booking.api.mapper.BookingApiMapper;
import com.uit.backend_cinema.modules.booking.domain.entity.PrePaymentBookingQuote;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/staff/bookings")
public class StaffBookingController {
    private final BookingService bookingService;
    private final BookingApiMapper bookingApiMapper;

    public StaffBookingController(BookingService bookingService, BookingApiMapper bookingApiMapper) {
        this.bookingService = bookingService;
        this.bookingApiMapper = bookingApiMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> createStaffBooking(
            @Valid @RequestBody CreateStaffBookingRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        PrePaymentBookingQuote quote = bookingService.createStaffBooking(user.getUserId(), requestDTO);
        CreateBookingResponseDTO response = bookingApiMapper.toResponse(quote);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo booking tại quầy thành công"));
    }

    @GetMapping("/customer-lookup")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> lookupCustomer(@RequestParam String email) {
        CustomerLookupResponseDTO response = bookingService.lookupCustomerByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response, "Tra cứu khách hàng thành công"));
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

    @GetMapping("/{bookingId}/cancellation-quote")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> getStaffCancellationQuote(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        var quote = bookingService.getCancellationQuote(bookingId, user.getUserId(), true);
        return ResponseEntity.ok(ApiResponse.success(quote));
    }

    @PostMapping("/{bookingId}/cancel")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> staffCancelBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) com.uit.backend_cinema.modules.booking.api.dto.CancelBookingRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        var response = bookingService.cancelBooking(bookingId, user.getUserId(), true, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
