package com.uit.backend_cinema.unit_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.JwtUtil;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.FoodFulfillmentStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.service.CinemaService;
import com.uit.backend_cinema.modules.cinema.domain.service.RoomService;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrder;
import com.uit.backend_cinema.modules.food_order.domain.entity.FoodOrderItem;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.entity.ShowtimeStatus;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInRequestDTO;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInResponseDTO;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.entity.TicketStatus;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketCheckInService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketCheckInServiceTest {

    private final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 10, 17, 10, 0, 0);

    @Mock private TicketRepository ticketRepository;
    @Mock private ShowtimeService showtimeService;
    @Mock private MovieService movieService;
    @Mock private RoomService roomService;
    @Mock private CinemaService cinemaService;
    @Mock private SeatRepository seatRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private FoodOrderService foodOrderService;
    private Clock clock;
    private TicketCheckInService ticketCheckInService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(fixedNow.atZone(zone).toInstant(), zone);

        ticketCheckInService = new TicketCheckInService(
                ticketRepository,
                showtimeService,
                movieService,
                roomService,
                cinemaService,
                seatRepository,
                bookingRepository,
                jwtUtil,
                foodOrderService,
                clock
        );
    }

    private Claims createValidClaims(Long ticketId, Long showtimeId, Long bookingId) {
        return Jwts.claims()
                .add("ticketId", ticketId)
                .add("showtimeId", showtimeId)
                .add("bookingId", bookingId)
                .build();
    }

    @Test
    @DisplayName("Check-in: ném lỗi nếu token QR không hợp lệ")
    void checkInRejectsInvalidToken() {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken("invalid-token");

        when(jwtUtil.validateTicketQrToken("invalid-token")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> ticketCheckInService.checkIn(request));
        assertEquals(ErrorCode.TICKET_INVALID_QR, exception.getCode());
    }

    @Test
    @DisplayName("Check-in: ném lỗi nếu vé đã được dùng")
    void checkInRejectsAlreadyCheckedInTicket() {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken("valid-token");

        when(jwtUtil.validateTicketQrToken("valid-token")).thenReturn(createValidClaims(1L, 2L, 3L));

        Booking booking = new Booking();
        booking.setBookingId(3L);
        booking.setStatus(BookingStatus.PAID);
        when(bookingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setShowtimeId(2L);
        ticket.setBookingId(3L);
        ticket.setStatus(TicketStatus.VALID);
        ticket.setIsCheckedIn(true); // Đã check-in

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        BusinessException exception = assertThrows(BusinessException.class, () -> ticketCheckInService.checkIn(request));
        assertEquals(ErrorCode.TICKET_ALREADY_USED, exception.getCode());
    }

    @Test
    @DisplayName("Check-in: ném lỗi nếu vé đã bị CANCELLED")
    void checkInRejectsCancelledTicket() {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken("valid-token");

        when(jwtUtil.validateTicketQrToken("valid-token")).thenReturn(createValidClaims(1L, 2L, 3L));

        Booking booking = new Booking();
        booking.setBookingId(3L);
        booking.setStatus(BookingStatus.PAID);
        when(bookingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setShowtimeId(2L);
        ticket.setBookingId(3L);
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setIsCheckedIn(false);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        BusinessException exception = assertThrows(BusinessException.class, () -> ticketCheckInService.checkIn(request));
        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getCode());
    }

    @Test
    @DisplayName("Check-in: ném lỗi nếu booking ở trạng thái CANCELLED")
    void checkInRejectsCancelledBooking() {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken("valid-token");

        when(jwtUtil.validateTicketQrToken("valid-token")).thenReturn(createValidClaims(1L, 2L, 3L));

        Booking booking = new Booking();
        booking.setBookingId(3L);
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));

        BusinessException exception = assertThrows(BusinessException.class, () -> ticketCheckInService.checkIn(request));
        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getCode());
    }

    @Test
    @DisplayName("Check-in: ném lỗi nếu check-in quá sớm (> 30 phút trước giờ chiếu)")
    void checkInRejectsTooEarly() {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken("valid-token");

        when(jwtUtil.validateTicketQrToken("valid-token")).thenReturn(createValidClaims(1L, 2L, 3L));

        Booking booking = new Booking();
        booking.setBookingId(3L);
        booking.setStatus(BookingStatus.PAID);
        when(bookingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setShowtimeId(2L);
        ticket.setBookingId(3L);
        ticket.setStatus(TicketStatus.VALID);
        ticket.setIsCheckedIn(false);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        Showtime showtime = new Showtime();
        // Chiếu lúc 11:00 (now là 10:00 -> cách 60 phút > 30 phút)
        showtime.setStartTime(fixedNow.plusMinutes(60));
        showtime.setEndTime(fixedNow.plusMinutes(180));
        when(showtimeService.getById(2L)).thenReturn(showtime);

        BusinessException exception = assertThrows(BusinessException.class, () -> ticketCheckInService.checkIn(request));
        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getCode());
    }

    @Test
    @DisplayName("Check-in: ném lỗi nếu suất chiếu đã kết thúc")
    void checkInRejectsEndedShowtime() {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken("valid-token");

        when(jwtUtil.validateTicketQrToken("valid-token")).thenReturn(createValidClaims(1L, 2L, 3L));

        Booking booking = new Booking();
        booking.setBookingId(3L);
        booking.setStatus(BookingStatus.PAID);
        when(bookingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setShowtimeId(2L);
        ticket.setBookingId(3L);
        ticket.setStatus(TicketStatus.VALID);
        ticket.setIsCheckedIn(false);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        Showtime showtime = new Showtime();
        showtime.setStartTime(fixedNow.minusHours(3));
        showtime.setEndTime(fixedNow.minusMinutes(10)); // Kết thúc 10 phút trước
        when(showtimeService.getById(2L)).thenReturn(showtime);

        BusinessException exception = assertThrows(BusinessException.class, () -> ticketCheckInService.checkIn(request));
        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getCode());
    }

    @Test
    @DisplayName("Check-in: thành công khi vé hợp lệ trong khung giờ [start - 30m, end) và giao đồ ăn lần đầu")
    void checkInSuccessAndFulfillFoodOnce() {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken("valid-token");

        when(jwtUtil.validateTicketQrToken("valid-token")).thenReturn(createValidClaims(1L, 2L, 3L));

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setShowtimeId(2L);
        ticket.setBookingId(3L);
        ticket.setSeatId(4L);
        ticket.setStatus(TicketStatus.VALID);
        ticket.setIsCheckedIn(false);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        Booking booking = new Booking();
        booking.setBookingId(3L);
        booking.setStatus(BookingStatus.PAID);
        booking.setFoodFulfillmentStatus(FoodFulfillmentStatus.PENDING);
        when(bookingRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));

        Showtime showtime = new Showtime();
        // Start sau 15 phút (trong khoảng start - 30m đến end)
        showtime.setStartTime(fixedNow.plusMinutes(15));
        showtime.setEndTime(fixedNow.plusMinutes(135));
        showtime.setMovieId(5L);
        showtime.setRoomId(6L);
        when(showtimeService.getById(2L)).thenReturn(showtime);

        Movie movie = new Movie();
        movie.setTitle("Avengers");
        when(movieService.getById(5L)).thenReturn(movie);

        Room room = new Room();
        room.setRoomName("IMAX 1");
        room.setCinemaId(7L);
        when(roomService.findById(6L)).thenReturn(room);

        Cinema cinema = new Cinema();
        cinema.setName("CAH Landmark 81");
        when(cinemaService.findById(7L)).thenReturn(cinema);

        Seat seat = new Seat();
        seat.setSeatRow(BigDecimal.valueOf(1)); // 'A'
        seat.setSeatCol(BigDecimal.valueOf(5)); // '5'
        when(seatRepository.findById(4L)).thenReturn(Optional.of(seat));

        FoodOrderItem item = new FoodOrderItem();
        item.setFoodId(11L);
        item.setFoodName("Bắp rang bơ");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("45000"));

        FoodOrder foodOrder = new FoodOrder();
        foodOrder.setBookingId(3L);
        foodOrder.setItems(List.of(item));
        when(foodOrderService.getByBookingId(3L)).thenReturn(Optional.of(foodOrder));

        CheckInResponseDTO response = ticketCheckInService.checkIn(request);

        assertNotNull(response);
        assertEquals("Avengers", response.getMovieTitle());
        assertEquals("CAH Landmark 81", response.getCinemaName());
        assertEquals("IMAX 1", response.getRoomName());
        assertEquals("A5", response.getSeatName());
        assertTrue(ticket.getIsCheckedIn());
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        assertEquals(FoodFulfillmentStatus.FULFILLED, booking.getFoodFulfillmentStatus());
        assertTrue(response.getFoodDeliveredNow());
        assertEquals(1, response.getFoodItems().size());
        assertEquals("Bắp rang bơ", response.getFoodItems().get(0).getFoodName());

        verify(ticketRepository).save(ticket);
        verify(bookingRepository).save(booking);

        // Giả lập vé thứ hai của cùng booking quét tiếp
        ticket.setIsCheckedIn(false); // vé 2 chưa check-in
        ticket.setTicketId(2L);
        when(jwtUtil.validateTicketQrToken("valid-token-2")).thenReturn(createValidClaims(2L, 2L, 3L));
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(ticket));

        CheckInRequestDTO request2 = new CheckInRequestDTO();
        request2.setQrToken("valid-token-2");
        CheckInResponseDTO response2 = ticketCheckInService.checkIn(request2);

        assertNotNull(response2);
        // Lần 2: đồ ăn đã FULFILLED trước đó nên không giao lại
        assertFalse(response2.getFoodDeliveredNow());
        assertEquals(FoodFulfillmentStatus.FULFILLED, response2.getFoodFulfillmentStatus());
    }
}
