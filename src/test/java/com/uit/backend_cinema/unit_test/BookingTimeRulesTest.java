package com.uit.backend_cinema.unit_test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.*;
import com.uit.backend_cinema.modules.booking.api.dto.*;
import com.uit.backend_cinema.modules.booking.domain.entity.*;
import com.uit.backend_cinema.modules.booking.domain.repository.*;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.seat.domain.entity.*;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.price_config.domain.service.PriceConfigService;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;

class BookingTimeRulesTest {
    final LocalDateTime start = DateTimeRulesTest.NOW;
    final Clock clock = mock(Clock.class);
    final BookingRepository bookings = mock(BookingRepository.class);
    final ShowtimeRepository showRepo = mock(ShowtimeRepository.class);
    final ShowtimeService shows = new ShowtimeService(showRepo, mock(MovieService.class), bookings, clock);
    final SeatService seats = mock(SeatService.class);
    final PriceConfigService prices = mock(PriceConfigService.class);
    final FoodOrderService foods = mock(FoodOrderService.class);
    final TicketService tickets = mock(TicketService.class);
    final VoucherService vouchers = mock(VoucherService.class);
    final PaymentConfirmationRepository payments = mock(PaymentConfirmationRepository.class);
    final OutboxEventService outbox = mock(OutboxEventService.class);
    final UserRepository users = mock(UserRepository.class);
    final Showtime show = DateTimeRulesTest.show(start, 120);
    final BookingService service = new BookingService(bookings, seats, shows, prices, tickets, foods,
            vouchers, payments, outbox, new ObjectMapper(), users, clock);
    Booking saved;

    BookingTimeRulesTest() {
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        time(start);
        when(showRepo.findByIdForUpdate(3L)).thenReturn(Optional.of(show));
        when(users.findById(7L)).thenReturn(Optional.of(new User()));
        SeatType type = new SeatType(); type.setPriceMultiplier(new BigDecimal("1.20"));
        Seat seat = new Seat(); seat.setSeatId(5L); seat.setSeatType(type);
        when(seats.promoteLocksForCheckout(3L, List.of(5L), 1L, 7L)).thenReturn(List.of(seat));
        when(prices.getPriceMultiplier(any(), any())).thenReturn(new BigDecimal("1.50"));
        when(foods.calculateDraftSubtotal(any())).thenReturn(new BigDecimal("30000"));
        when(vouchers.applyVoucherForBooking(any(), any())).thenReturn(BigDecimal.ZERO);
        when(bookings.save(any())).thenAnswer(a -> {
            saved = a.getArgument(0); saved.setBookingId(9L); return saved;
        });
    }
    void time(LocalDateTime now) { when(clock.instant()).thenReturn(now.toInstant(ZoneOffset.UTC)); }
    CreateBookingRequestDTO request() {
        var r = new CreateBookingRequestDTO(); r.setShowtimeId(3L); r.setSeatIds(List.of(5L));
        r.setPaymentMethod(BookingPaymentMethod.CASH); return r;
    }
    ConfirmPaymentRequestDTO payment() {
        var p = new ConfirmPaymentRequestDTO(); p.setPaymentRef("REF"); p.setGateway("CASH"); return p;
    }
    void money(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }

    @ParameterizedTest @CsvSource({"14,59,0,205800", "15,0,0,205800", "15,1,88200,117600", "45,0,88200,117600"})
    void fixesPriceAtBookingAndDiscountsOnlyTickets(int minute, int second, String discount, String total) {
        time(start.plusMinutes(minute).plusSeconds(second));
        var q = service.createPrePaymentBooking(7L, request());
        money("176400", q.getSeatSubtotal()); money("29400", q.getFoodSubtotal());
        money(discount, q.getLateDiscountAmount()); money(discount, q.getDiscountAmount());
        money(total, q.getTotalAmount()); money("0", q.getVoucherDiscountAmount());
        assertEquals(start.plusMinutes(minute + 15).plusSeconds(second), q.getExpiresAt());
    }
    @Test void ut04PersistsPendingDeadlineAndCompletePriceSnapshot() {
        time(start.minusHours(1));
        var r = request();
        r.setVoucherId(4L);
        when(vouchers.applyVoucherForBooking(eq(4L), any())).thenReturn(new BigDecimal("20000"));
        List<Booking> persisted = new ArrayList<>();
        ObjectMapper snapshotMapper = new ObjectMapper().findAndRegisterModules();
        doAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setBookingId(9L);
            // Copy at the repository boundary; later mutation cannot satisfy persistence assertions.
            persisted.add(snapshotMapper.convertValue(booking, Booking.class));
            return booking;
        }).when(bookings).save(any());

        var quote = service.createPrePaymentBooking(7L, r);

        verify(bookings, times(2)).save(any());
        assertEquals(2, persisted.size());
        assertEquals(BookingStatus.PENDING, persisted.get(0).getStatus());
        assertEquals(start.minusMinutes(45), persisted.get(0).getExpiresAt());
        Booking saved = persisted.get(1);
        assertEquals(BookingStatus.PENDING, saved.getStatus());
        assertEquals(BookingStatus.PENDING, quote.getStatus());
        assertEquals(7L, saved.getUserId());
        assertEquals(3L, saved.getShowtimeId());
        assertEquals(start.minusMinutes(45), saved.getExpiresAt());
        assertEquals(start.minusMinutes(45), saved.getPaymentDeadline());
        assertEquals(start.minusMinutes(45).plusSeconds(30), saved.getReconciliationDeadline());
        assertEquals(start.minusMinutes(45), quote.getExpiresAt());
        money("180000", saved.getSeatSubtotalGross());
        money("30000", saved.getFoodSubtotalGross());
        money("4200", saved.getRankDiscountAmount());
        money("0", saved.getLateDiscountAmount());
        money("17142.86", saved.getVoucherDiscountSeat());
        money("2857.14", saved.getVoucherDiscountFood());
        money("159257.14", saved.getNetSeatAmount());
        money("26542.86", saved.getNetFoodAmount());
        money("20000", saved.getDiscountAmount());
        money("185800", saved.getTotalAmount());
        money("185800", quote.getTotalAmount());
        verifyNoInteractions(payments, outbox);
    }

    @Test void ut06ExpiryAtDeadlineUsesConditionalUpdateBeforeReleasingResources() {
        Booking pending = new Booking();
        pending.setBookingId(9L); pending.setUserId(7L); pending.setShowtimeId(3L);
        pending.setVoucherId(4L); pending.setStatus(BookingStatus.PENDING);
        pending.setExpiresAt(start);
        when(bookings.findByStatusAndExpiresAtLessThanEqual(BookingStatus.PENDING, start))
                .thenReturn(List.of(pending));
        when(bookings.markExpiredIfPendingAndExpired(9L, start)).thenReturn(1);
        when(tickets.findActiveDraftSeatIds(9L)).thenReturn(List.of(5L));

        service.expirePendingBookings();

        var order = inOrder(bookings, tickets, seats, vouchers, foods);
        order.verify(bookings).findByStatusAndExpiresAtLessThanEqual(BookingStatus.PENDING, start);
        order.verify(bookings).markExpiredIfPendingAndExpired(9L, start);
        order.verify(tickets).findActiveDraftSeatIds(9L);
        order.verify(seats).releaseSeatLocksByOwner(3L, List.of(5L), 7L);
        order.verify(vouchers).releaseVoucherForExpiredBooking(4L);
        order.verify(tickets).expireDraftItems(9L);
        order.verify(foods).expireDraftItems(9L);
        verify(bookings, never()).save(any());
        verifyNoInteractions(payments, outbox);
        // EXPIRED is written by the repository's conditional SQL, not this detached object.
    }

    @Test void ut06PaymentWinningExpiryRacePreventsAllResourceCleanup() {
        Booking paid = new Booking();
        paid.setBookingId(9L); paid.setStatus(BookingStatus.PAID); paid.setExpiresAt(start);
        // A stale scan may still contain a booking whose payment has since won the race.
        when(bookings.findByStatusAndExpiresAtLessThanEqual(BookingStatus.PENDING, start))
                .thenReturn(List.of(paid));
        when(bookings.markExpiredIfPendingAndExpired(9L, start)).thenReturn(0);

        service.expirePendingBookings();

        assertEquals(BookingStatus.PAID, paid.getStatus());
        verify(bookings).findByStatusAndExpiresAtLessThanEqual(BookingStatus.PENDING, start);
        verify(bookings).markExpiredIfPendingAndExpired(9L, start);
        verify(bookings, never()).save(any());
        verifyNoInteractions(seats, vouchers, tickets, foods, payments, outbox);
    }

    @Test void latePriceRoundsHalfUp() {
        time(start.plusMinutes(16)); show.setBasePrice(new BigDecimal("0.05"));
        money("0.04", service.createPrePaymentBooking(7L, request()).getLateDiscountAmount());
    }
    @Test void normalBookingStillAppliesVoucherAndReportsBreakdown() {
        time(start.plusMinutes(14)); var r = request(); r.setVoucherId(4L);
        when(vouchers.applyVoucherForBooking(eq(4L), any())).thenReturn(new BigDecimal("20000"));
        var quote = service.createPrePaymentBooking(7L, r);
        money("0", quote.getLateDiscountAmount()); money("20000", quote.getVoucherDiscountAmount());
        money("20000", quote.getDiscountAmount()); money("185800", quote.getTotalAmount());
    }

    @Test void lateVoucherRejectedWithoutDatabaseOrVoucherWrites() {
        time(start.plusMinutes(16)); var r = request(); r.setVoucherId(4L);
        assertEquals(ErrorCode.DISCOUNT_NOT_COMBINABLE,
                assertThrows(BusinessException.class, () -> service.createPrePaymentBooking(7L, r)).getCode());
        verify(bookings, never()).save(any()); verifyNoInteractions(vouchers, tickets, foods);
        verify(seats).releaseSeatLocksByOwner(3L, List.of(5L), 7L);
    }
    @Test void closedWindowDoesNotPromoteLocks() {
        time(start.plusMinutes(45).plusNanos(1));
        assertThrows(BusinessException.class, () -> service.createPrePaymentBooking(7L, request()));
        verifyNoInteractions(seats, vouchers); verify(bookings, never()).save(any());
    }
    @Test void windowClosingDuringPromotionReleasesLocks() {
        time(start.plusMinutes(45));
        when(seats.promoteLocksForCheckout(anyLong(), anyList(), anyLong(), anyLong())).thenAnswer(a -> {
            time(start.plusMinutes(45).plusSeconds(1)); return List.of();
        });
        assertThrows(BusinessException.class, () -> service.createPrePaymentBooking(7L, request()));
        verify(seats).releaseSeatLocksByOwner(3L, List.of(5L), 7L);
        verify(bookings, never()).save(any());
    }
    @Test void paymentAfterMinuteFifteenKeepsOriginalPrice() {
        time(start.plusMinutes(14)); service.createPrePaymentBooking(7L, request());
        when(bookings.findByIdForUpdate(9L)).thenReturn(Optional.of(saved));
        time(start.plusMinutes(16));
        assertEquals(BookingStatus.PAID, service.confirmPayment(7L, 9L, payment()).getStatus());
        money("205800", saved.getTotalAmount()); money("0", saved.getLateDiscountAmount());
    }
    @ParameterizedTest @CsvSource({"-1,true", "0,false", "1,false"})
    void paymentRechecksTimeAfterLock(int expiryOffsetSeconds, boolean accepted) {
        time(start); service.createPrePaymentBooking(7L, request());
        when(bookings.findByIdForUpdate(9L)).thenAnswer(a -> {
            time(saved.getExpiresAt().plusSeconds(expiryOffsetSeconds)); return Optional.of(saved);
        });
        if (accepted) assertEquals(BookingStatus.PAID, service.confirmPayment(7L, 9L, payment()).getStatus());
        else {
            assertEquals(ErrorCode.BOOKING_EXPIRED,
                    assertThrows(BusinessException.class, () -> service.confirmPayment(7L, 9L, payment())).getCode());
            assertEquals(BookingStatus.PENDING, saved.getStatus());
            verify(payments, never()).save(any()); verifyNoInteractions(outbox);
            verify(tickets, never()).finalizeTicketsForPaidBooking(anyLong(), anyLong());
        }
    }
    @Test void successfulPaymentReplayWorksAfterExpiry() {
        service.createPrePaymentBooking(7L, request()); saved.setStatus(BookingStatus.PAID);
        var confirmation = new PaymentConfirmation(); confirmation.setBookingId(9L);
        confirmation.setPaymentRef("REF"); confirmation.setGateway("CASH");
        when(payments.findByPaymentRef("REF")).thenReturn(Optional.of(confirmation));
        when(bookings.findById(9L)).thenReturn(Optional.of(saved));
        time(start.plusHours(3));
        assertEquals(BookingStatus.PAID, service.confirmPayment(7L, 9L, payment()).getStatus());
        verify(bookings, never()).findByIdForUpdate(anyLong());
    }
}
