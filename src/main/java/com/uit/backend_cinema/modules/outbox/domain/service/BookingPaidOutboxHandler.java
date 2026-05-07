package com.uit.backend_cinema.modules.outbox.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.PendingTicketItem;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingFoodDraftItemRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingVoucherHoldRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.PendingTicketItemRepository;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.payload.BookingPaidPayload;
import com.uit.backend_cinema.modules.outbox.domain.payload.SendTicketEmailPayload;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookingPaidOutboxHandler {
    private final ObjectMapper objectMapper;
    private final BookingRepository bookingRepository;
    private final PendingTicketItemRepository pendingTicketItemRepository;
    private final BookingFoodDraftItemRepository bookingFoodDraftItemRepository;
    private final BookingVoucherHoldRepository bookingVoucherHoldRepository;
    private final TicketService ticketService;
    private final OutboxEventService outboxEventService;

    public BookingPaidOutboxHandler(ObjectMapper objectMapper,
                                    BookingRepository bookingRepository,
                                    PendingTicketItemRepository pendingTicketItemRepository,
                                    BookingFoodDraftItemRepository bookingFoodDraftItemRepository,
                                    BookingVoucherHoldRepository bookingVoucherHoldRepository,
                                    TicketService ticketService,
                                    OutboxEventService outboxEventService) {
        this.objectMapper = objectMapper;
        this.bookingRepository = bookingRepository;
        this.pendingTicketItemRepository = pendingTicketItemRepository;
        this.bookingFoodDraftItemRepository = bookingFoodDraftItemRepository;
        this.bookingVoucherHoldRepository = bookingVoucherHoldRepository;
        this.ticketService = ticketService;
        this.outboxEventService = outboxEventService;
    }

    @Transactional
    public void handle(OutboxEvent event) {
        BookingPaidPayload payload = readPayload(event.getPayload());
        Booking booking = bookingRepository.findById(payload.getBookingId())
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));

        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BusinessException("Booking chưa thanh toán, không thể tạo vé", ErrorCode.BOOKING_INVALID_STATUS);
        }

        List<PendingTicketItem> pendingItems = pendingTicketItemRepository.findAllActiveByBookingId(booking.getBookingId());
        List<Ticket> tickets = new ArrayList<>();
        for (PendingTicketItem item : pendingItems) {
            Ticket ticket = new Ticket();
            ticket.setBookingId(item.getBookingId());
            ticket.setSeatId(item.getSeatId());
            ticket.setPrice(item.getUnitPrice());
            tickets.add(ticket);
        }

        if (tickets.isEmpty()) {
            tickets = ticketService.findAllByBookingId(booking.getBookingId());
            if (tickets.isEmpty()) {
                throw new BusinessException("Booking không có vé nháp để tạo vé", ErrorCode.RESOURCE_NOT_FOUND);
            }
        } else {
            ticketService.createTicketIfAbsent(tickets);
        }

        SendTicketEmailPayload emailPayload = new SendTicketEmailPayload();
        emailPayload.setBookingId(booking.getBookingId());
        emailPayload.setUserId(booking.getUserId());

        outboxEventService.createIfAbsent(
                OutboxEventType.SEND_TICKET_EMAIL,
                booking.getBookingId().toString(),
                writePayload(emailPayload)
        );

        pendingTicketItemRepository.softDeleteByBookingId(booking.getBookingId());
        bookingFoodDraftItemRepository.softDeleteByBookingId(booking.getBookingId());
        bookingVoucherHoldRepository.softDeleteByBookingId(booking.getBookingId());
        outboxEventService.markDone(event.getOutboxEventId());
    }

    private BookingPaidPayload readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, BookingPaidPayload.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Payload BOOKING_PAID không hợp lệ", ErrorCode.VALIDATION_FAILED, ex);
        }
    }

    private String writePayload(SendTicketEmailPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Không thể tạo payload SEND_TICKET_EMAIL", ErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, ex);
        }
    }
}