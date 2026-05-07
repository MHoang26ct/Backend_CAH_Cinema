package com.uit.backend_cinema.modules.outbox.domain.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.payload.BookingPaidPayload;
import com.uit.backend_cinema.modules.outbox.domain.payload.SendTicketEmailPayload;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;

@Service
public class BookingPaidOutboxHandler {
    private final ObjectMapper objectMapper;
    private final BookingRepository bookingRepository;
    private final TicketService ticketService;
    private final FoodOrderService foodOrderService;
    private final VoucherService voucherService;
    private final OutboxEventService outboxEventService;

    public BookingPaidOutboxHandler(ObjectMapper objectMapper,
                                    BookingRepository bookingRepository,
                                    TicketService ticketService,
                                    FoodOrderService foodOrderService,
                                    VoucherService voucherService,
                                    OutboxEventService outboxEventService) {
        this.objectMapper = objectMapper;
        this.bookingRepository = bookingRepository;
        this.ticketService = ticketService;
        this.foodOrderService = foodOrderService;
        this.voucherService = voucherService;
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

        try {
            ticketService.finalizeTicketsForPaidBooking(booking.getBookingId(), booking.getShowtimeId());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Ghế đã được bán cho suất chiếu này", ErrorCode.SEAT_ALREADY_BOOKED, ex);
        }

        SendTicketEmailPayload emailPayload = new SendTicketEmailPayload();
        emailPayload.setBookingId(booking.getBookingId());
        emailPayload.setUserId(booking.getUserId());

        outboxEventService.createIfAbsent(
                OutboxEventType.SEND_TICKET_EMAIL,
                booking.getBookingId().toString(),
                writePayload(emailPayload)
        );

        ticketService.expireDraftItems(booking.getBookingId());
        foodOrderService.expireDraftItems(booking.getBookingId());
        voucherService.softDeleteHold(booking.getBookingId());
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
