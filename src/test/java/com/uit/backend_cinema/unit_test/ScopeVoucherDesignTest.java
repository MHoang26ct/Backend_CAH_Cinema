package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
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
import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import com.uit.backend_cinema.modules.voucher.domain.entity.VoucherType;
import com.uit.backend_cinema.modules.voucher.domain.repository.VoucherRepository;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Fixed-clock unit contracts; atomic SQL and concurrent expiry require integration tests. */
class ScopeVoucherDesignTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
    private final LocalDateTime now = LocalDateTime.now(clock);
    private final VoucherRepository vouchers = mock(VoucherRepository.class);
    private final VoucherService service = new VoucherService(vouchers, clock);

    @Test
    void ut08InactiveVoucherIsRejectedWithoutConsumption() {
        Voucher voucher = validVoucher();
        voucher.setIsActive(false);
        when(vouchers.findById(22L)).thenReturn(Optional.of(voucher));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.applyVoucherForBooking(22L, new BigDecimal("150000")));

        assertEquals(ErrorCode.VALIDATION_FAILED, error.getCode());
        verify(vouchers).findById(22L);
        verifyNoMoreInteractions(vouchers);
    }

    @Test
    void ut08ExhaustedVoucherAtomicRejectionDoesNotReturnDiscount() {
        Voucher voucher = validVoucher();
        voucher.setUsedCount(voucher.getQuantity());
        when(vouchers.findById(22L)).thenReturn(Optional.of(voucher));
        // Exhaustion is enforced by the repository's conditional update, not the service's snapshot check.
        when(vouchers.consumeVoucherAtomically(22L, now)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.applyVoucherForBooking(22L, new BigDecimal("150000")));

        assertEquals(ErrorCode.VALIDATION_FAILED, error.getCode());
        verify(vouchers).findById(22L);
        verify(vouchers).consumeVoucherAtomically(22L, now);
        verifyNoMoreInteractions(vouchers);
    }

    @Test
    void ut08BelowMinimumAmountIsRejectedWithoutConsumption() {
        when(vouchers.findById(22L)).thenReturn(Optional.of(validVoucher()));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.applyVoucherForBooking(22L, new BigDecimal("99999")));

        assertEquals(ErrorCode.VALIDATION_FAILED, error.getCode());
        verify(vouchers).findById(22L);
        verifyNoMoreInteractions(vouchers);
    }

    @Test
    void ut08ExactMinimumAmountIsAcceptedAndConsumedOnce() {
        when(vouchers.findById(22L)).thenReturn(Optional.of(validVoucher()));
        when(vouchers.consumeVoucherAtomically(22L, now)).thenReturn(1);

        assertEquals(new BigDecimal("30000"), service.applyVoucherForBooking(22L, new BigDecimal("100000")));

        verify(vouchers).findById(22L);
        verify(vouchers).consumeVoucherAtomically(22L, now);
        verifyNoMoreInteractions(vouchers);
    }

    @Test
    void ut09NullVoucherReleaseIsRepeatableNoOp() {
        service.releaseVoucherForExpiredBooking(null);
        service.releaseVoucherForExpiredBooking(null);

        verifyNoInteractions(vouchers);
    }

    @ParameterizedTest(name = "UT09: first expiry claim result {0}, repeated claim returns zero")
    @ValueSource(ints = {0, 1})
    void ut09RepeatedExpiryReleasesResourcesOnlyForSuccessfulClaim(int firstClaimResult) {
        BookingRepository bookings = mock(BookingRepository.class);
        SeatService seats = mock(SeatService.class);
        TicketService tickets = mock(TicketService.class);
        FoodOrderService foodOrders = mock(FoodOrderService.class);
        BookingService bookingService = new BookingService(
                bookings, seats, mock(ShowtimeService.class), mock(PriceConfigService.class),
                tickets, foodOrders, service, mock(PaymentConfirmationRepository.class),
                mock(OutboxEventService.class), new ObjectMapper(), mock(UserRepository.class), clock);
        Booking booking = new Booking();
        booking.setBookingId(10L);
        booking.setShowtimeId(7L);
        booking.setUserId(50L);
        booking.setVoucherId(22L);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(now.minusMinutes(1));
        // Return the same stale candidate twice to exercise the guard, not merely an empty second scan.
        when(bookings.findByStatusAndExpiresAtLessThanEqual(BookingStatus.PENDING, now))
                .thenReturn(List.of(booking));
        when(bookings.markExpiredIfPendingAndExpired(10L, now)).thenReturn(firstClaimResult, 0);
        if (firstClaimResult == 1) {
            when(tickets.findActiveDraftSeatIds(10L)).thenReturn(List.of(1L, 2L));
            when(vouchers.releaseVoucherAtomically(22L)).thenReturn(1);
        }

        bookingService.expirePendingBookings();
        bookingService.expirePendingBookings();

        verify(bookings, times(2)).findByStatusAndExpiresAtLessThanEqual(BookingStatus.PENDING, now);
        verify(bookings, times(2)).markExpiredIfPendingAndExpired(10L, now);
        verify(vouchers, times(firstClaimResult)).releaseVoucherAtomically(22L);
        verify(seats, times(firstClaimResult)).releaseSeatLocksByOwner(7L, List.of(1L, 2L), 50L);
        verify(tickets, times(firstClaimResult)).findActiveDraftSeatIds(10L);
        verify(tickets, times(firstClaimResult)).expireDraftItems(10L);
        verify(foodOrders, times(firstClaimResult)).expireDraftItems(10L);
        verifyNoMoreInteractions(bookings, vouchers, seats, tickets, foodOrders);
    }

    private Voucher validVoucher() {
        Voucher voucher = new Voucher();
        voucher.setVoucherId(22L);
        voucher.setType(VoucherType.FIXED_AMOUNT);
        voucher.setValue(new BigDecimal("30000"));
        voucher.setMinOrderValue(new BigDecimal("100000"));
        voucher.setQuantity(10);
        voucher.setUsedCount(0);
        voucher.setIsActive(true);
        voucher.setStartAt(now.minusDays(1));
        voucher.setExpiredAt(now.plusDays(1));
        return voucher;
    }
}
