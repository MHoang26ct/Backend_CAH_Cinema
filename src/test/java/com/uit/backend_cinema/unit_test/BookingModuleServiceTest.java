package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.PaymentConfirmationRepository;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.price_config.domain.service.PriceConfigService;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingModuleServiceTest {

    @Test
    @DisplayName("Booking module: hết hạn booking pending sẽ dọn lock, draft ticket, draft food và hoàn voucher usage")
    void expirePendingBookingsReleasesDraftResources() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SeatService seatService = mock(SeatService.class);
        ShowtimeService showtimeService = mock(ShowtimeService.class);
        PriceConfigService priceConfigService = mock(PriceConfigService.class);
        TicketService ticketService = mock(TicketService.class);
        FoodOrderService foodOrderService = mock(FoodOrderService.class);
        VoucherService voucherService = mock(VoucherService.class);
        UserRepository userRepository = mock(UserRepository.class);
        PaymentConfirmationRepository paymentConfirmationRepository = mock(PaymentConfirmationRepository.class);
        OutboxEventService outboxEventService = mock(OutboxEventService.class);
        BookingService bookingService = new BookingService(
                bookingRepository,
                seatService,
                showtimeService,
                priceConfigService,
                ticketService,
                foodOrderService,
                voucherService,
                paymentConfirmationRepository,
                outboxEventService,
                new ObjectMapper(),
                userRepository
        );
        Booking booking = new Booking();
        booking.setBookingId(10L);
        booking.setShowtimeId(20L);
        booking.setUserId(30L);
        booking.setVoucherId(99L);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(bookingRepository.findByStatusAndExpiresAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingRepository.markExpiredIfPendingAndExpired(eq(10L), any(LocalDateTime.class))).thenReturn(1);
        when(ticketService.findActiveDraftSeatIds(10L)).thenReturn(List.of(1L, 2L));

        bookingService.expirePendingBookings();

        verify(seatService).releaseSeatLocksByOwner(20L, List.of(1L, 2L), 30L);
        verify(voucherService).releaseVoucherForExpiredBooking(99L);
        verify(ticketService).expireDraftItems(10L);
        verify(foodOrderService).expireDraftItems(10L);
    }

    @Test
    @DisplayName("refundBookingsForCancelledShowtime: Refund booking PAID và Expire booking PENDING thành công")
    void refundBookingsForCancelledShowtime_success() throws Exception {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        SeatService seatService = mock(SeatService.class);
        ShowtimeService showtimeService = mock(ShowtimeService.class);
        PriceConfigService priceConfigService = mock(PriceConfigService.class);
        TicketService ticketService = mock(TicketService.class);
        FoodOrderService foodOrderService = mock(FoodOrderService.class);
        VoucherService voucherService = mock(VoucherService.class);
        UserRepository userRepository = mock(UserRepository.class);
        PaymentConfirmationRepository paymentConfirmationRepository = mock(PaymentConfirmationRepository.class);
        OutboxEventService outboxEventService = mock(OutboxEventService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        
        BookingService bookingService = new BookingService(
                bookingRepository, seatService, showtimeService, priceConfigService,
                ticketService, foodOrderService, voucherService, paymentConfirmationRepository,
                outboxEventService, objectMapper, userRepository
        );

        Long showtimeId = 50L;

        // Mock PAID booking
        Booking paidBooking = new Booking();
        paidBooking.setBookingId(101L);
        paidBooking.setStatus(BookingStatus.PAID);
        paidBooking.setUserId(201L);
        paidBooking.setVoucherId(301L);
        paidBooking.setShowtimeId(showtimeId);

        // Mock PENDING booking
        Booking pendingBooking = new Booking();
        pendingBooking.setBookingId(102L);
        pendingBooking.setStatus(BookingStatus.PENDING);
        pendingBooking.setUserId(202L);
        pendingBooking.setVoucherId(302L);
        pendingBooking.setShowtimeId(showtimeId);

        when(bookingRepository.findActiveByShowtimeId(showtimeId))
                .thenReturn(List.of(paidBooking, pendingBooking));
        when(bookingRepository.markExpiredIfPendingAndExpired(eq(102L), any(LocalDateTime.class))).thenReturn(1);
        when(ticketService.findActiveDraftSeatIds(102L)).thenReturn(List.of(1L, 2L));

        // Call method
        bookingService.refundBookingsForCancelledShowtime(showtimeId, "Bảo trì phòng chiếu");

        // Asserts for PAID booking
        assertEquals(BookingStatus.REFUNDED, paidBooking.getStatus());
        verify(bookingRepository).save(paidBooking);
        verify(voucherService).releaseVoucherForExpiredBooking(301L);
        verify(outboxEventService).createIfAbsent(
                eq(com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType.SHOWTIME_CANCELLED),
                eq("booking-101"),
                anyString()
        );

        // Asserts for PENDING booking
        verify(bookingRepository).markExpiredIfPendingAndExpired(eq(102L), any(LocalDateTime.class));
        verify(seatService).releaseSeatLocksByOwner(showtimeId, List.of(1L, 2L), 202L);
        verify(voucherService).releaseVoucherForExpiredBooking(302L);
        verify(ticketService).expireDraftItems(102L);
        verify(foodOrderService).expireDraftItems(102L);
    }
}
