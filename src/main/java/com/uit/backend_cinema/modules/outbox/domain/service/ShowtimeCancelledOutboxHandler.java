package com.uit.backend_cinema.modules.outbox.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
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
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;

@Service
public class ShowtimeCancelledOutboxHandler {
    private final ObjectMapper objectMapper;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final NotificationService notificationService;
    private final OutboxEventService outboxEventService;

    public ShowtimeCancelledOutboxHandler(ObjectMapper objectMapper,
                                          BookingRepository bookingRepository,
                                          UserRepository userRepository,
                                          ShowtimeService showtimeService,
                                          MovieService movieService,
                                          NotificationService notificationService,
                                          OutboxEventService outboxEventService) {
        this.objectMapper = objectMapper;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.showtimeService = showtimeService;
        this.movieService = movieService;
        this.notificationService = notificationService;
        this.outboxEventService = outboxEventService;
    }

    @Transactional
    public void handle(OutboxEvent event) {
        ShowtimeCancelledPayload payload = readPayload(event.getPayload());

        Booking booking = bookingRepository.findById(payload.getBookingId())
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));

        // Chỉ gửi email nếu booking đã được refund
        if (booking.getStatus() != BookingStatus.REFUNDED) {
            outboxEventService.markDone(event.getOutboxEventId());
            return;
        }

        User user = userRepository.findById(payload.getUserId())
                .orElseThrow(() -> new BusinessException("User không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));

        Showtime showtime = showtimeService.getById(payload.getShowtimeId());
        Movie movie = movieService.getById(showtime.getMovieId());

        notificationService.sendShowtimeCancelledEmail(
                user.getEmail(),
                booking.getBookingId(),
                movie,
                showtime,
                payload.getReason()
        );

        outboxEventService.markDone(event.getOutboxEventId());
    }

    private ShowtimeCancelledPayload readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, ShowtimeCancelledPayload.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Payload SHOWTIME_CANCELLED không hợp lệ", ErrorCode.VALIDATION_FAILED, ex);
        }
    }
}
