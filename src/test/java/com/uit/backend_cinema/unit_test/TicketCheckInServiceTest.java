package com.uit.backend_cinema.unit_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.JwtUtil;
import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.service.CinemaService;
import com.uit.backend_cinema.modules.cinema.domain.service.RoomService;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInRequestDTO;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInResponseDTO;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketCheckInService;

class TicketCheckInServiceTest {

    private TicketRepository ticketRepository;
    private ShowtimeService showtimeService;
    private MovieService movieService;
    private RoomService roomService;
    private CinemaService cinemaService;
    private SeatRepository seatRepository;
    private JwtUtil jwtUtil;
    private TicketCheckInService ticketCheckInService;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        showtimeService = mock(ShowtimeService.class);
        movieService = mock(MovieService.class);
        roomService = mock(RoomService.class);
        cinemaService = mock(CinemaService.class);
        seatRepository = mock(SeatRepository.class);
        jwtUtil = mock(JwtUtil.class);

        ticketCheckInService = new TicketCheckInService(
                ticketRepository,
                showtimeService,
                movieService,
                roomService,
                cinemaService,
                seatRepository,
                jwtUtil
        );
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

        Claims claims = mock(Claims.class);
        when(claims.get("ticketId", Number.class)).thenReturn(1L);
        when(claims.get("showtimeId", Number.class)).thenReturn(2L);
        when(claims.get("bookingId", Number.class)).thenReturn(3L);
        when(jwtUtil.validateTicketQrToken("valid-token")).thenReturn(claims);

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setShowtimeId(2L);
        ticket.setBookingId(3L);
        ticket.setIsCheckedIn(true); // Đã check-in

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        BusinessException exception = assertThrows(BusinessException.class, () -> ticketCheckInService.checkIn(request));
        assertEquals(ErrorCode.TICKET_ALREADY_USED, exception.getCode());
    }

    @Test
    @DisplayName("Check-in: ném lỗi nếu suất chiếu đã kết thúc hơn 4 tiếng trước")
    void checkInRejectsEndedShowtime() {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken("valid-token");

        Claims claims = mock(Claims.class);
        when(claims.get("ticketId", Number.class)).thenReturn(1L);
        when(claims.get("showtimeId", Number.class)).thenReturn(2L);
        when(claims.get("bookingId", Number.class)).thenReturn(3L);
        when(jwtUtil.validateTicketQrToken("valid-token")).thenReturn(claims);

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setShowtimeId(2L);
        ticket.setBookingId(3L);
        ticket.setIsCheckedIn(false);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        Showtime showtime = new Showtime();
        showtime.setStartTime(LocalDateTime.now().minusHours(8));
        showtime.setEndTime(LocalDateTime.now().minusHours(5)); // Kết thúc 5 tiếng trước

        when(showtimeService.getById(2L)).thenReturn(showtime);

        BusinessException exception = assertThrows(BusinessException.class, () -> ticketCheckInService.checkIn(request));
        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getCode());
    }

    @Test
    @DisplayName("Check-in: thành công khi vé hợp lệ")
    void checkInSuccess() {
        CheckInRequestDTO request = new CheckInRequestDTO();
        request.setQrToken("valid-token");

        Claims claims = mock(Claims.class);
        when(claims.get("ticketId", Number.class)).thenReturn(1L);
        when(claims.get("showtimeId", Number.class)).thenReturn(2L);
        when(claims.get("bookingId", Number.class)).thenReturn(3L);
        when(jwtUtil.validateTicketQrToken("valid-token")).thenReturn(claims);

        Ticket ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setShowtimeId(2L);
        ticket.setBookingId(3L);
        ticket.setSeatId(4L);
        ticket.setIsCheckedIn(false);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        Showtime showtime = new Showtime();
        showtime.setStartTime(LocalDateTime.now().minusHours(1));
        showtime.setEndTime(LocalDateTime.now().plusHours(1));
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
        seat.setSeatType(new com.uit.backend_cinema.modules.seat.domain.entity.SeatType()); // mock seat type
        when(seatRepository.findById(4L)).thenReturn(Optional.of(seat));

        CheckInResponseDTO response = ticketCheckInService.checkIn(request);

        assertNotNull(response);
        assertEquals("Avengers", response.getMovieTitle());
        assertEquals("CAH Landmark 81", response.getCinemaName());
        assertEquals("IMAX 1", response.getRoomName());
        assertEquals("A5", response.getSeatName());
        assertTrue(ticket.getIsCheckedIn());

        verify(ticketRepository).save(ticket);
    }
}
