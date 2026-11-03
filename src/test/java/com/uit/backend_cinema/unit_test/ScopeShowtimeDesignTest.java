package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.entity.ShowtimeStatus;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScopeShowtimeDesignTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 30);
    private ShowtimeRepository repository;
    private ShowtimeService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShowtimeRepository.class);
        MovieService movies = mock(MovieService.class);
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Clock clock = Clock.fixed(DATE.minusDays(1).atTime(10, 0).atZone(zone).toInstant(), zone);
        service = new ShowtimeService(repository, movies, mock(BookingRepository.class), clock);
        Movie movie = new Movie();
        movie.setDuration(120);
        when(movies.getById(2L)).thenReturn(movie);
        when(repository.findAllByRoomIdAndDate(eq(1L), any(LocalDate.class))).thenReturn(List.of());

        Showtime existing = showtime(DATE.atTime(12, 0));
        existing.setShowtimeId(99L);
        when(repository.findAllByRoomIdAndDate(1L, DATE)).thenReturn(List.of(existing));
    }

    @ParameterizedTest(name = "new showtime starts {0} seconds relative to existing start")
    @ValueSource(longs = {-9000, 9000})
    @DisplayName("UT-17: exactly 30 minutes before or after another showtime is accepted")
    void createShowtimeAcceptsExactlyThirtyMinuteBuffer(long startOffsetSeconds) {
        Showtime candidate = showtime(DATE.atTime(12, 0).plusSeconds(startOffsetSeconds));

        assertDoesNotThrow(() -> service.createShowtime(candidate));

        verify(repository).save(candidate);
        assertEquals(candidate.getStartTime().plusMinutes(120), candidate.getEndTime());
        assertEquals(ShowtimeStatus.AVAILABLE, candidate.getStatus());
    }

    @ParameterizedTest(name = "new showtime starts {0} seconds relative to existing start")
    @ValueSource(longs = {-8999, 8999, -7200, 7200})
    @DisplayName("UT-17: 29m59s and zero-minute gaps are rejected in both directions")
    void createShowtimeRejectsUnderThirtyMinuteBuffer(long startOffsetSeconds) {
        Showtime candidate = showtime(DATE.atTime(12, 0).plusSeconds(startOffsetSeconds));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createShowtime(candidate));

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getCode());
        assertEquals("Khoảng cách giữa 2 suất chiếu phải ít nhất 30 phút", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest(name = "new showtime starts {0} seconds relative to existing start")
    @ValueSource(longs = {-7199, 0, 7199})
    @DisplayName("UT-17: one-second overlap in either direction and identical intervals are rejected")
    void createShowtimeRejectsOverlappingIntervals(long startOffsetSeconds) {
        Showtime candidate = showtime(DATE.atTime(12, 0).plusSeconds(startOffsetSeconds));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createShowtime(candidate));

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getCode());
        assertEquals("Suất chiếu trùng lặp với suất chiếu hiện có", exception.getMessage());
        verify(repository, never()).save(any());
    }

    private Showtime showtime(LocalDateTime start) {
        Showtime showtime = new Showtime();
        showtime.setRoomId(1L);
        showtime.setMovieId(2L);
        showtime.setFormat(MovieFormat.TYPE_2D);
        showtime.setBasePrice(new BigDecimal("100000"));
        showtime.setStartTime(start);
        showtime.setEndTime(start.plusMinutes(120));
        showtime.setStatus(ShowtimeStatus.AVAILABLE);
        showtime.setIsDeleted(false);
        return showtime;
    }
}
