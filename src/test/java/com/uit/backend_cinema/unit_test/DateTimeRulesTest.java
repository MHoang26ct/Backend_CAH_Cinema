package com.uit.backend_cinema.unit_test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.uit.backend_cinema.common.exception.*;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.showtime.domain.entity.*;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.voucher.domain.entity.*;
import com.uit.backend_cinema.modules.voucher.domain.repository.VoucherRepository;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;

class DateTimeRulesTest {
    static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 21, 10, 0);
    static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    final ShowtimeRepository repo = mock(ShowtimeRepository.class);
    final MovieService movies = mock(MovieService.class);
    final BookingRepository bookings = mock(BookingRepository.class);
    final ShowtimeService service = new ShowtimeService(repo, movies, bookings, CLOCK);

    DateTimeRulesTest() {
        Movie movie = new Movie(); movie.setDuration(120);
        when(movies.getById(2L)).thenReturn(movie);
    }

    static Showtime show(LocalDateTime start, int minutes) {
        Showtime s = new Showtime();
        s.setShowtimeId(3L); s.setMovieId(2L); s.setRoomId(1L);
        s.setStartTime(start); s.setEndTime(start.plusMinutes(minutes));
        s.setOriginalDurationMicros(minutes * 60_000_000L);
        s.setFormat(MovieFormat.TYPE_2D); s.setStatus(ShowtimeStatus.AVAILABLE);
        s.setBasePrice(new BigDecimal("100000")); s.setIsDeleted(false);
        return s;
    }

    @Test void derivesEndAndIgnoresClientDuration() {
        Showtime s = show(NOW.plusDays(1), 10);
        service.createShowtime(s);
        assertEquals(s.getStartTime().plusHours(2), s.getEndTime());
        assertEquals(120 * 60_000_000L, s.getOriginalDurationMicros());
        verify(repo).save(s);
    }

    @ParameterizedTest @CsvSource({"-1", "0"})
    void rejectsNonFutureStart(long seconds) {
        assertEquals(ErrorCode.VALIDATION_FAILED, assertThrows(BusinessException.class,
                () -> service.createShowtime(show(NOW.plusSeconds(seconds), 120))).getCode());
        verify(repo, never()).save(any());
    }

    @Test void futureBoundaryAndOvernightStillWork() {
        service.createShowtime(show(NOW.plusDays(30), 120));
        assertThrows(BusinessException.class, () -> service.createShowtime(show(NOW.plusDays(31), 120)));
        Showtime late = show(NOW.plusDays(1).withHour(23), 120);
        service.createShowtime(late);
        assertEquals(late.getStartTime().plusDays(1).toLocalDate(), late.getEndTime().toLocalDate());
        assertEquals(1, late.getEndTime().getHour());
    }

    @ParameterizedTest @CsvSource({"44,59,true", "45,0,true", "45,1,false", "120,0,false"})
    void lastCheckoutBoundary(int minutes, int seconds, boolean allowed) {
        Showtime s = show(NOW, 120);
        LocalDateTime candidate = NOW.plusMinutes(minutes).plusSeconds(seconds);
        if (allowed) assertDoesNotThrow(() -> service.validateBookingWindow(s, candidate));
        else assertEquals(ErrorCode.SHOWTIME_BOOKING_CLOSED, assertThrows(BusinessException.class,
                () -> service.validateBookingWindow(s, candidate)).getCode());
    }

    @Test void shortOddAndOvernightDurationsUseExactMidpoint() {
        Showtime shortFilm = show(NOW, 15);
        service.validateBookingWindow(shortFilm, NOW.minusMinutes(7).minusSeconds(30));
        assertThrows(BusinessException.class, () -> service.validateBookingWindow(shortFilm, NOW));
        Showtime odd = show(NOW.withHour(23), 121);
        LocalDateTime last = odd.getStartTime().plusMinutes(45).plusSeconds(30);
        service.validateBookingWindow(odd, last);
        assertThrows(BusinessException.class, () -> service.validateBookingWindow(odd, last.plusNanos(1)));
    }

    @Test void reschedulingKeepsSnapshotWhenMovieDurationChanges() {
        Showtime original = show(NOW.plusDays(1), 120);
        when(repo.findByIdForUpdate(3L)).thenReturn(Optional.of(original));
        Movie longer = new Movie(); longer.setDuration(150);
        when(movies.getById(2L)).thenReturn(longer);
        Showtime update = show(original.getStartTime().plusHours(1), 150);
        service.updateShowtime(update);
        assertEquals(120 * 60_000_000L, update.getOriginalDurationMicros());
        assertEquals(update.getStartTime().plusMinutes(120), update.getEndTime());
    }

    @Test void bookedOrStartedScheduleCannotMove() {
        Showtime original = show(NOW.plusDays(1), 120);
        when(repo.findByIdForUpdate(3L)).thenReturn(Optional.of(original));
        when(bookings.hasScheduleBlockingBookings(3L, NOW)).thenReturn(true);
        assertEquals(ErrorCode.SHOWTIME_SCHEDULE_LOCKED, assertThrows(BusinessException.class,
                () -> service.updateShowtime(show(NOW.plusDays(2), 120))).getCode());
        original.setStartTime(NOW.minusMinutes(1));
        assertThrows(BusinessException.class, () -> service.updateShowtime(show(NOW.plusDays(2), 120)));
        verify(repo, never()).save(any());
    }

    @Test void holdsRejectClosedShowtime() {
        when(repo.findById(3L)).thenReturn(Optional.of(show(NOW.minusHours(3), 120)));
        assertEquals(ErrorCode.SHOWTIME_BOOKING_CLOSED,
                assertThrows(BusinessException.class, () -> service.getBookableById(3L)).getCode());
    }

    @Test void voucherRequiresPositiveValidityAndExcludesExpiry() {
        VoucherRepository vr = mock(VoucherRepository.class);
        VoucherService vs = new VoucherService(vr, CLOCK);
        Voucher v = new Voucher();
        v.setVoucherId(1L); v.setCode("TEST"); v.setType(VoucherType.FIXED_AMOUNT);
        v.setValue(BigDecimal.TEN); v.setMinOrderValue(BigDecimal.ZERO); v.setIsActive(true);
        v.setStartAt(NOW); v.setExpiredAt(NOW);
        assertThrows(BusinessException.class, () -> vs.create(v));
        verify(vr, never()).save(any());
        v.setStartAt(NOW.minusDays(1));
        when(vr.findById(1L)).thenReturn(Optional.of(v));
        assertThrows(BusinessException.class, () -> vs.applyVoucherForBooking(1L, BigDecimal.TEN));
        verify(vr, never()).consumeVoucherAtomically(anyLong(), any());
        v.setStartAt(NOW); v.setExpiredAt(NOW.plusDays(1));
        when(vr.consumeVoucherAtomically(1L, NOW)).thenReturn(1);
        assertEquals(BigDecimal.TEN, vs.applyVoucherForBooking(1L, BigDecimal.TEN));
    }
}
