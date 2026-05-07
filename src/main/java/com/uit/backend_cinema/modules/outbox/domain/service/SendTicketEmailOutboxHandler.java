package com.uit.backend_cinema.modules.outbox.domain.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SendTicketEmailOutboxHandler {
    private final ObjectMapper objectMapper;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    private final OutboxEventService outboxEventService;

    public SendTicketEmailOutboxHandler(ObjectMapper objectMapper,
                                        BookingRepository bookingRepository,
                                        UserRepository userRepository,
                                        TicketService ticketService,
                                        NotificationService notificationService,
                                        OutboxEventService outboxEventService) {
        this.objectMapper = objectMapper;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.notificationService = notificationService;
        this.outboxEventService = outboxEventService;
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

        notificationService.sendTicketEmail(user.getEmail(), booking.getBookingId(), tickets);
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
