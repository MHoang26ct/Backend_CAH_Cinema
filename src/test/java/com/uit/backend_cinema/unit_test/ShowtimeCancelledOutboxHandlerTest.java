package com.uit.backend_cinema.unit_test;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.notification.domain.service.NotificationService;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.payload.ShowtimeCancelledPayload;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.outbox.domain.service.ShowtimeCancelledOutboxHandler;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowtimeCancelledOutboxHandlerTest {

    @Test
    @DisplayName("handle: Gửi email thành công nếu booking đã được REFUNDED")
    void handle_bookingRefunded() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        BookingRepository bookingRepository = mock(BookingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ShowtimeService showtimeService = mock(ShowtimeService.class);
        MovieService movieService = mock(MovieService.class);
        NotificationService notificationService = mock(NotificationService.class);
        OutboxEventService outboxEventService = mock(OutboxEventService.class);

        ShowtimeCancelledOutboxHandler handler = new ShowtimeCancelledOutboxHandler(
                objectMapper, bookingRepository, userRepository, showtimeService,
                movieService, notificationService, outboxEventService
        );

        ShowtimeCancelledPayload payload = new ShowtimeCancelledPayload();
        payload.setBookingId(1L);
        payload.setUserId(2L);
        payload.setShowtimeId(3L);
        payload.setReason("Lý do test");

        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(100L);
        event.setPayload(objectMapper.writeValueAsString(payload));

        Booking booking = new Booking();
        booking.setBookingId(1L);
        booking.setStatus(BookingStatus.REFUNDED); // quan trọng
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        User user = new User();
        user.setUserId(2L);
        user.setEmail("test@example.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        Showtime showtime = new Showtime();
        showtime.setMovieId(4L);
        when(showtimeService.getById(3L)).thenReturn(showtime);

        Movie movie = new Movie();
        movie.setMovieId(4L);
        movie.setTitle("Phim Test");
        when(movieService.getById(4L)).thenReturn(movie);

        // Execute
        handler.handle(event);

        // Verify email sent
        verify(notificationService).sendShowtimeCancelledEmail(
                eq("test@example.com"), eq(1L), eq(movie), eq(showtime), eq("Lý do test")
        );
        // Verify marked done
        verify(outboxEventService).markDone(100L);
    }

    @Test
    @DisplayName("handle: Bỏ qua gửi email nếu booking chưa phải REFUNDED")
    void handle_bookingNotRefunded() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        BookingRepository bookingRepository = mock(BookingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ShowtimeService showtimeService = mock(ShowtimeService.class);
        MovieService movieService = mock(MovieService.class);
        NotificationService notificationService = mock(NotificationService.class);
        OutboxEventService outboxEventService = mock(OutboxEventService.class);

        ShowtimeCancelledOutboxHandler handler = new ShowtimeCancelledOutboxHandler(
                objectMapper, bookingRepository, userRepository, showtimeService,
                movieService, notificationService, outboxEventService
        );

        ShowtimeCancelledPayload payload = new ShowtimeCancelledPayload();
        payload.setBookingId(1L);

        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(100L);
        event.setPayload(objectMapper.writeValueAsString(payload));

        Booking booking = new Booking();
        booking.setBookingId(1L);
        booking.setStatus(BookingStatus.PAID); // KHÁC REFUNDED
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // Execute
        handler.handle(event);

        // Verify email NOT sent
        verify(notificationService, never()).sendShowtimeCancelledEmail(anyString(), any(), any(), any(), anyString());
        // Verify still marked done (do ta chỉ gửi cho REFUNDED, PENDING thì k gửi, ko cần retry)
        verify(outboxEventService).markDone(100L);
    }
}
