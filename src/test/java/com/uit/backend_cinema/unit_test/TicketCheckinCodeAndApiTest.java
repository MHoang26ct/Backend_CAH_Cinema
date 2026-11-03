package com.uit.backend_cinema.unit_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.common.util.CheckinCodeUtil;
import com.uit.backend_cinema.common.util.JwtUtil;
import com.uit.backend_cinema.modules.auth.domain.entity.AccountType;
import com.uit.backend_cinema.modules.booking.api.controller.BookingController;
import com.uit.backend_cinema.modules.booking.api.dto.BookingStatusResponseDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.service.CinemaService;
import com.uit.backend_cinema.modules.cinema.domain.service.RoomService;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.entity.ShowtimeStatus;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInRequestDTO;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInResponseDTO;
import com.uit.backend_cinema.modules.ticket.api.dto.TicketItemResponseDTO;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.entity.TicketStatus;
import com.uit.backend_cinema.modules.ticket.domain.repository.PendingTicketItemRepository;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketCheckInService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;

@ExtendWith(MockitoExtension.class)
class TicketCheckinCodeAndApiTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private PendingTicketItemRepository pendingTicketItemRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private ShowtimeService showtimeService;
    @Mock private MovieService movieService;
    @Mock private RoomService roomService;
    @Mock private CinemaService cinemaService;
    @Mock private com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository bookingRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private FoodOrderService foodOrderService;
    @Mock private BookingService bookingService;

    private Clock fixedClock;
    private TicketCheckInService checkInService;
    private TicketService ticketService;
    private BookingController bookingController;

    private static final String ZONE = "Asia/Ho_Chi_Minh";

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneId.of(ZONE));
        checkInService = new TicketCheckInService(
                ticketRepository,
                showtimeService,
                movieService,
                roomService,
                cinemaService,
                seatRepository,
                bookingRepository,
                jwtUtil,
                foodOrderService,
                fixedClock
        );
        ticketService = new TicketService(ticketRepository, pendingTicketItemRepository, seatRepository);
        bookingController = new BookingController(bookingService, null, ticketService);
    }

    @Test
    @DisplayName("CheckinCodeUtil sinh mã đúng định dạng TK-XXXX-XXXX và không chứa ký tự dễ nhầm lẫn")
    void testCheckinCodeUtilFormatAndEntropy() {
        String code1 = CheckinCodeUtil.generateCode();
        String code2 = CheckinCodeUtil.generateCode();

        assertNotNull(code1);
        assertTrue(code1.matches("^TK-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$"), "Format must be TK-XXXX-XXXX: " + code1);
        assertNotEquals(code1, code2);
        assertFalse(code1.contains("0") || code1.contains("O") || code1.contains("1") || code1.contains("I"));
    }

    @Test
    @DisplayName("Check-in bằng mã rút gọn checkinCode thành công")
    void testCheckInWithShortCheckinCodeSuccess() {
        String code = "TK-9M4K-2P7Q";
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken(code);

        when(jwtUtil.validateTicketQrToken(code)).thenReturn(null);

        Ticket ticket = new Ticket();
        ticket.setTicketId(10L);
        ticket.setBookingId(20L);
        ticket.setShowtimeId(30L);
        ticket.setSeatId(40L);
        ticket.setCheckinCode(code);
        ticket.setStatus(TicketStatus.VALID);
        ticket.setIsCheckedIn(false);

        when(ticketRepository.findByCheckinCode(code)).thenReturn(Optional.of(ticket));

        Booking booking = new Booking();
        booking.setBookingId(20L);
        booking.setStatus(BookingStatus.PAID);
        when(bookingRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(booking));

        LocalDateTime now = LocalDateTime.now(fixedClock);
        Showtime showtime = new Showtime();
        showtime.setShowtimeId(30L);
        showtime.setMovieId(50L);
        showtime.setRoomId(60L);
        showtime.setStatus(ShowtimeStatus.AVAILABLE);
        showtime.setStartTime(now.plusMinutes(10)); // 10 phút nữa chiếu -> hợp lệ
        showtime.setEndTime(now.plusHours(2));
        when(showtimeService.getById(30L)).thenReturn(showtime);

        Movie movie = new Movie();
        movie.setTitle("Avatar 3");
        when(movieService.getById(50L)).thenReturn(movie);

        Room room = new Room();
        room.setRoomName("Phòng 01");
        room.setCinemaId(70L);
        when(roomService.findById(60L)).thenReturn(room);

        Cinema cinema = new Cinema();
        cinema.setName("CAH Cinema Thủ Đức");
        when(cinemaService.findById(70L)).thenReturn(cinema);

        Seat seat = new Seat();
        seat.setSeatId(40L);
        seat.setSeatRow(new BigDecimal("1")); // A
        seat.setSeatCol(new BigDecimal("8")); // 8 -> A8
        when(seatRepository.findById(40L)).thenReturn(Optional.of(seat));

        CheckInResponseDTO response = checkInService.checkIn(request);

        assertNotNull(response);
        assertEquals(10L, response.getTicketId());
        assertEquals("A8", response.getSeatName());
        assertEquals("Avatar 3", response.getMovieTitle());
        assertTrue(ticket.getIsCheckedIn());
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("TicketService.getTicketDetailsByBookingId trả về danh sách vé có checkinCode và format đúng tên ghế")
    void testGetTicketDetailsByBookingId() {
        Ticket t1 = new Ticket();
        t1.setTicketId(101L);
        t1.setBookingId(200L);
        t1.setShowtimeId(300L);
        t1.setSeatId(1L);
        t1.setPrice(new BigDecimal("85000"));
        t1.setCheckinCode("TK-AAAA-BBBB");
        t1.setStatus(TicketStatus.VALID);
        t1.setIsCheckedIn(false);

        when(ticketRepository.findAllByBookingId(200L)).thenReturn(List.of(t1));

        Seat seat = new Seat();
        seat.setSeatId(1L);
        seat.setSeatRow(new BigDecimal("2")); // B
        seat.setSeatCol(new BigDecimal("5")); // 5 -> B5
        when(seatRepository.findByIds(List.of(1L))).thenReturn(List.of(seat));

        List<TicketItemResponseDTO> details = ticketService.getTicketDetailsByBookingId(200L);

        assertEquals(1, details.size());
        TicketItemResponseDTO item = details.get(0);
        assertEquals(101L, item.getTicketId());
        assertEquals("B5", item.getSeatName());
        assertEquals("TK-AAAA-BBBB", item.getCheckinCode());
        assertEquals("TK-AAAA-BBBB", item.getQrCode());
        assertEquals(new BigDecimal("85000"), item.getPrice());
    }

    @Test
    @DisplayName("BookingController: GET /api/v1/bookings/{id} trả về danh sách vé khi status = PAID")
    @SuppressWarnings("unchecked")
    void testGetBookingStatusIncludesTicketsWhenPaid() {
        Booking booking = new Booking();
        booking.setBookingId(199L);
        booking.setStatus(BookingStatus.PAID);
        booking.setUserId(17L);

        when(bookingService.getBooking(199L, 17L)).thenReturn(booking);

        Ticket ticket = new Ticket();
        ticket.setTicketId(94L);
        ticket.setBookingId(199L);
        ticket.setSeatId(1077L);
        ticket.setPrice(new BigDecimal("170500"));
        ticket.setCheckinCode("TK-2B07-C9C4");
        ticket.setStatus(TicketStatus.VALID);
        ticket.setIsCheckedIn(false);

        when(ticketRepository.findAllByBookingId(199L)).thenReturn(List.of(ticket));

        Seat seat = new Seat();
        seat.setSeatId(1077L);
        seat.setSeatRow(new BigDecimal("5")); // E
        seat.setSeatCol(new BigDecimal("10")); // 10 -> E10
        when(seatRepository.findByIds(List.of(1077L))).thenReturn(List.of(seat));

        CustomUserDetails user = new CustomUserDetails(
                17L, "user@test.com", "pass",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        ResponseEntity<?> responseEntity = bookingController.getBookingStatus(199L, user);
        assertNotNull(responseEntity);
        ApiResponse<BookingStatusResponseDTO> apiResponse = (ApiResponse<BookingStatusResponseDTO>) responseEntity.getBody();
        assertNotNull(apiResponse);
        BookingStatusResponseDTO dto = apiResponse.getData();
        assertEquals(199L, dto.getBookingId());
        assertEquals(BookingStatus.PAID, dto.getStatus());
        assertNotNull(dto.getTickets());
        assertEquals(1, dto.getTickets().size());
        assertEquals("TK-2B07-C9C4", dto.getTickets().get(0).getCheckinCode());
        assertEquals("E10", dto.getTickets().get(0).getSeatName());
    }

    @Test
    @DisplayName("BookingController: GET /api/v1/bookings/{id}/tickets trả về trực tiếp danh sách vé")
    @SuppressWarnings("unchecked")
    void testGetBookingTicketsEndpoint() {
        Booking booking = new Booking();
        booking.setBookingId(199L);
        booking.setUserId(17L);
        when(bookingService.getBooking(199L, 17L)).thenReturn(booking);

        Ticket ticket = new Ticket();
        ticket.setTicketId(94L);
        ticket.setBookingId(199L);
        ticket.setSeatId(1077L);
        ticket.setPrice(new BigDecimal("170500"));
        ticket.setCheckinCode("TK-2B07-C9C4");
        ticket.setStatus(TicketStatus.VALID);
        ticket.setIsCheckedIn(false);

        when(ticketRepository.findAllByBookingId(199L)).thenReturn(List.of(ticket));

        Seat seat = new Seat();
        seat.setSeatId(1077L);
        seat.setSeatRow(new BigDecimal("1"));
        seat.setSeatCol(new BigDecimal("1"));
        when(seatRepository.findByIds(List.of(1077L))).thenReturn(List.of(seat));

        CustomUserDetails user = new CustomUserDetails(
                17L, "user@test.com", "pass",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        ResponseEntity<?> responseEntity = bookingController.getBookingTickets(199L, user);
        assertNotNull(responseEntity);
        ApiResponse<List<TicketItemResponseDTO>> apiResponse = (ApiResponse<List<TicketItemResponseDTO>>) responseEntity.getBody();
        assertNotNull(apiResponse);
        List<TicketItemResponseDTO> tickets = apiResponse.getData();
        assertEquals(1, tickets.size());
        assertEquals("TK-2B07-C9C4", tickets.get(0).getCheckinCode());
    }
}
