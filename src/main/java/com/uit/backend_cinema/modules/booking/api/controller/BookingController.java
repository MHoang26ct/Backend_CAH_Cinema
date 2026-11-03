package com.uit.backend_cinema.modules.booking.api.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.booking.api.dto.BookingStatusResponseDTO;
import com.uit.backend_cinema.modules.booking.api.dto.CreateBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.api.mapper.BookingApiMapper;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.PrePaymentBookingQuote;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.ticket.api.dto.TicketItemResponseDTO;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final BookingApiMapper bookingApiMapper;
    private final TicketService ticketService;

    public BookingController(BookingService bookingService,
                             BookingApiMapper bookingApiMapper,
                             TicketService ticketService) {
        this.bookingService = bookingService;
        this.bookingApiMapper = bookingApiMapper;
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody CreateBookingRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (requestDTO.getPaymentMethod() == com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod.CASH) {
            throw new com.uit.backend_cinema.common.exception.BusinessException(
                    "Thanh toán tiền mặt chỉ áp dụng tại quầy",
                    com.uit.backend_cinema.common.exception.ErrorCode.CASH_PAYMENT_REQUIRES_STAFF);
        }
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(user.getUserId(), requestDTO);
        var response = bookingApiMapper.toResponse(quote);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo booking chờ thanh toán thành công"));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingStatus(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal CustomUserDetails user) {
        Booking booking = bookingService.getBooking(bookingId, user.getUserId());
        List<TicketItemResponseDTO> tickets = null;
        if (booking.getStatus() == BookingStatus.PAID || booking.getStatus() == BookingStatus.CHECKED_IN) {
            tickets = ticketService.getTicketDetailsByBookingId(bookingId);
        }
        BookingStatusResponseDTO response = BookingStatusResponseDTO.builder()
                .bookingId(booking.getBookingId())
                .status(booking.getStatus())
                .tickets(tickets)
                .build();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy trạng thái booking thành công"));
    }

    @GetMapping("/{bookingId}/tickets")
    public ResponseEntity<?> getBookingTickets(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal CustomUserDetails user) {
        bookingService.getBooking(bookingId, user.getUserId());
        List<TicketItemResponseDTO> tickets = ticketService.getTicketDetailsByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success(tickets, "Lấy danh sách vé thành công"));
    }

    @GetMapping("/{id}/cancellation-quote")
    public ResponseEntity<ApiResponse<com.uit.backend_cinema.modules.booking.api.dto.CancellationQuoteDTO>> getCancellationQuote(
            @PathVariable("id") Long bookingId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var quote = bookingService.getCancellationQuote(bookingId, userDetails.getUserId(), false);
        return ResponseEntity.ok(ApiResponse.success(quote));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<com.uit.backend_cinema.modules.booking.api.dto.CancelBookingResponseDTO>> cancelBooking(
            @PathVariable("id") Long bookingId,
            @RequestBody(required = false) com.uit.backend_cinema.modules.booking.api.dto.CancelBookingRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var response = bookingService.cancelBooking(bookingId, userDetails.getUserId(), false, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
