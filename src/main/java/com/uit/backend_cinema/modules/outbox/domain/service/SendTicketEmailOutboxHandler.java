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
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.notification.domain.service.NotificationService;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.payload.SendTicketEmailPayload;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;

@Service
public class SendTicketEmailOutboxHandler {
    private final ObjectMapper objectMapper;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    private final OutboxEventService outboxEventService;
    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final SeatService seatService;

    public SendTicketEmailOutboxHandler(ObjectMapper objectMapper,
                                        BookingRepository bookingRepository,
                                        UserRepository userRepository,
                                        TicketService ticketService,
                                        NotificationService notificationService,
                                        OutboxEventService outboxEventService,
                                        ShowtimeService showtimeService,
                                        MovieService movieService,
                                        SeatService seatService) {
        this.objectMapper = objectMapper;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.notificationService = notificationService;
        this.outboxEventService = outboxEventService;
        this.showtimeService = showtimeService;
        this.movieService = movieService;
        this.seatService = seatService;
    }

    @Transactional
    public void handle(OutboxEvent event) {
        SendTicketEmailPayload payload = readPayload(event.getPayload());
        Booking booking = bookingRepository.findById(payload.getBookingId())
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        User user = userRepository.findById(payload.getUserId())
                .orElseThrow(() -> new BusinessException("User không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        List<Ticket> tickets = ticketService.findAllByBookingId(booking.getBookingId());

        if (tickets.isEmpty()) {
            throw new BusinessException("Booking chưa có vé để gửi email", ErrorCode.RESOURCE_NOT_FOUND);
        }

        Showtime showtime = showtimeService.getById(tickets.get(0).getShowtimeId());
        Movie movie = movieService.getById(showtime.getMovieId());
        List<Seat> seats = seatService.findByIds(tickets.stream().map(Ticket::getSeatId).toList());

        notificationService.sendTicketEmail(user.getEmail(), booking.getBookingId(), movie, showtime, tickets, seats);
        outboxEventService.markDone(event.getOutboxEventId());
    }

    private SendTicketEmailPayload readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, SendTicketEmailPayload.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Payload SEND_TICKET_EMAIL không hợp lệ", ErrorCode.VALIDATION_FAILED, ex);
        }
    }
}
