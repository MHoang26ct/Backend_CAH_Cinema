package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.modules.auth.domain.entity.AccountType;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.invoice.domain.service.InvoiceService;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.notification.domain.service.NotificationService;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.service.BookingPaidOutboxHandler;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.outbox.domain.service.SendTicketEmailOutboxHandler;
import com.uit.backend_cinema.modules.outbox.domain.service.ShowtimeCancelledOutboxHandler;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutboxAndPointsHandlerTest {

    private ObjectMapper objectMapper;

    @Mock private BookingRepository bookingRepository;
    @Mock private TicketService ticketService;
    @Mock private InvoiceService invoiceService;
    @Mock private FoodOrderService foodOrderService;
    @Mock private OutboxEventService outboxEventService;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ShowtimeService showtimeService;
    @Mock private MovieService movieService;
    @Mock private SeatService seatService;

    private BookingPaidOutboxHandler bookingPaidOutboxHandler;
    private SendTicketEmailOutboxHandler sendTicketEmailOutboxHandler;
    private ShowtimeCancelledOutboxHandler showtimeCancelledOutboxHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        bookingPaidOutboxHandler = new BookingPaidOutboxHandler(
                objectMapper, bookingRepository, ticketService, invoiceService,
                foodOrderService, outboxEventService, userRepository
        );

        sendTicketEmailOutboxHandler = new SendTicketEmailOutboxHandler(
                objectMapper, bookingRepository, userRepository, ticketService,
                notificationService, outboxEventService, showtimeService, movieService, seatService
        );

        showtimeCancelledOutboxHandler = new ShowtimeCancelledOutboxHandler(
                objectMapper, bookingRepository, userRepository, showtimeService,
                movieService, notificationService, outboxEventService
        );
    }

    @Test
    @DisplayName("BOOKING_PAID: Khi booking đã CANCELLED, không cấp vé, không gửi email, đảo điểm nếu có, đánh dấu DONE")
    void testBookingPaidHandlerWhenBookingAlreadyCancelled() throws Exception {
        Long bookingId = 1L;
        Long userId = 10L;

        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setIsPointsAccumulated(true); // Đã tích lũy trước đó
        booking.setTotalAmount(new BigDecimal("100000"));

        User user = new User();
        user.setUserId(userId);
        user.setAccountType(AccountType.REGISTERED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(101L);
        event.setPayload("{\"bookingId\":1,\"userId\":10}");

        bookingPaidOutboxHandler.handle(event);

        // Không cấp vé
        verify(ticketService, never()).finalizeTicketsForPaidBooking(any(), any());
        // Không tạo SEND_TICKET_EMAIL
        verify(outboxEventService, never()).createIfAbsent(eq(OutboxEventType.SEND_TICKET_EMAIL), any(), any());
        // Khấu trừ điểm vì đơn đã hủy
        verify(userRepository).deductPaidAndRecalcRank(userId, new BigDecimal("100000"));
        assertFalse(booking.getIsPointsAccumulated());
        verify(outboxEventService).markDone(101L);
    }

    @Test
    @DisplayName("BOOKING_PAID: Khi booking đã CHECKED_IN, tích lũy điểm và tạo invoice an toàn, không báo lỗi trạng thái")
    void testBookingPaidHandlerWhenBookingAlreadyCheckedIn() throws Exception {
        Long bookingId = 2L;
        Long userId = 20L;

        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setPaymentMethod(BookingPaymentMethod.VNPAY);
        booking.setIsPointsAccumulated(false);
        booking.setTotalAmount(new BigDecimal("150000"));

        User user = new User();
        user.setUserId(userId);
        user.setAccountType(AccountType.REGISTERED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(102L);
        event.setPayload("{\"bookingId\":2,\"userId\":20}");

        bookingPaidOutboxHandler.handle(event);

        verify(invoiceService).createInvoice(bookingId, "VNPAY", new BigDecimal("150000"));
        verify(userRepository).accumulatePaidAndRecalcRank(userId, new BigDecimal("150000"));
        assertTrue(booking.getIsPointsAccumulated());
        verify(outboxEventService).markDone(102L);

        // Replay: Chạy lại lần thứ 2 không cộng điểm lặp
        bookingPaidOutboxHandler.handle(event);
        verify(userRepository, times(1)).accumulatePaidAndRecalcRank(any(), any());
    }

    @Test
    @DisplayName("SEND_TICKET_EMAIL: Không gửi email vé khi booking ở trạng thái CANCELLED")
    void testSendTicketEmailSkipsWhenCancelled() {
        Long bookingId = 3L;
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(201L);
        event.setPayload("{\"bookingId\":3,\"userId\":30}");

        sendTicketEmailOutboxHandler.handle(event);

        verify(notificationService, never()).sendTicketEmail(any(), any(), any(), any(), any(), any());
        verify(outboxEventService).markDone(201L);
    }

    @Test
    @DisplayName("SEND_TICKET_EMAIL: Khách WALK_IN không có contact_email thì bỏ qua email mà không báo lỗi")
    void testSendTicketEmailSkipsWalkInWithoutEmail() {
        Long bookingId = 4L;
        Long walkInUserId = 999L;

        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUserId(walkInUserId);
        booking.setStatus(BookingStatus.PAID);
        booking.setContactEmail(null); // không để lại email

        User walkInUser = new User();
        walkInUser.setUserId(walkInUserId);
        walkInUser.setAccountType(AccountType.WALK_IN);
        walkInUser.setEmail("walkin@cinema.local");

        Ticket ticket = new Ticket();
        ticket.setTicketId(401L);
        ticket.setBookingId(bookingId);
        ticket.setShowtimeId(501L);
        ticket.setSeatId(601L);

        Showtime showtime = new Showtime();
        showtime.setShowtimeId(501L);
        showtime.setMovieId(701L);

        Movie movie = new Movie();
        Seat seat = new Seat();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(walkInUserId)).thenReturn(Optional.of(walkInUser));
        when(ticketService.findAllByBookingId(bookingId)).thenReturn(List.of(ticket));
        when(showtimeService.getById(501L)).thenReturn(showtime);
        when(movieService.getById(701L)).thenReturn(movie);
        when(seatService.findByIds(any())).thenReturn(List.of(seat));

        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(202L);
        event.setPayload("{\"bookingId\":4,\"userId\":999}");

        sendTicketEmailOutboxHandler.handle(event);

        // Không gửi email cho walk-in email giả
        verify(notificationService, never()).sendTicketEmail(any(), any(), any(), any(), any(), any());
        verify(outboxEventService).markDone(202L);
    }

    @Test
    @DisplayName("SHOWTIME_CANCELLED: Khách WALK_IN không có email thì bỏ qua mà không ném lỗi")
    void testShowtimeCancelledSkipsWalkInWithoutEmail() {
        Long bookingId = 5L;
        Long walkInUserId = 999L;

        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUserId(walkInUserId);
        booking.setStatus(BookingStatus.REFUNDED);
        booking.setContactEmail(null);

        User walkInUser = new User();
        walkInUser.setUserId(walkInUserId);
        walkInUser.setAccountType(AccountType.WALK_IN);
        walkInUser.setEmail("walkin@cinema.local");

        Showtime showtime = new Showtime();
        showtime.setShowtimeId(801L);
        showtime.setMovieId(901L);

        Movie movie = new Movie();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(userRepository.findById(walkInUserId)).thenReturn(Optional.of(walkInUser));
        when(showtimeService.getById(801L)).thenReturn(showtime);
        when(movieService.getById(901L)).thenReturn(movie);

        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(301L);
        event.setPayload("{\"bookingId\":5,\"userId\":999,\"showtimeId\":801,\"reason\":\"Sự cố điện\"}");

        showtimeCancelledOutboxHandler.handle(event);

        verify(notificationService, never()).sendShowtimeCancelledEmail(any(), any(), any(), any(), any());
        verify(outboxEventService).markDone(301L);
    }
}
