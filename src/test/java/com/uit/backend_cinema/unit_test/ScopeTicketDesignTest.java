package com.uit.backend_cinema.unit_test;

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
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScopeTicketDesignTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 30, 12, 0);
    private TicketRepository tickets;
    private BookingRepository bookings;
    private ShowtimeService showtimes;
    private MovieService movies;
    private RoomService rooms;
    private CinemaService cinemas;
    private SeatRepository seats;
    private JwtUtil jwt;
    private Ticket ticket;
    private Booking booking;
    private CheckInRequestDTO request;

    @BeforeEach
    void setUp() {
        tickets = mock(TicketRepository.class);
        bookings = mock(BookingRepository.class);
        showtimes = mock(ShowtimeService.class);
        movies = mock(MovieService.class);
        rooms = mock(RoomService.class);
        cinemas = mock(CinemaService.class);
        seats = mock(SeatRepository.class);
        jwt = mock(JwtUtil.class);
        request = new CheckInRequestDTO();
        request.setQrToken("valid-token");
        when(jwt.validateTicketQrToken("valid-token")).thenReturn(Jwts.claims()
                .add("ticketId", 1L).add("showtimeId", 2L).add("bookingId", 3L).build());

        booking = new Booking();
        booking.setBookingId(3L);
        booking.setStatus(BookingStatus.PAID);
        booking.setFoodFulfillmentStatus(FoodFulfillmentStatus.PENDING);
        when(bookings.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));

        ticket = new Ticket();
        ticket.setTicketId(1L);
        ticket.setShowtimeId(2L);
        ticket.setBookingId(3L);
        ticket.setSeatId(4L);
        ticket.setStatus(TicketStatus.VALID);
        ticket.setIsCheckedIn(false);
        when(tickets.findById(1L)).thenReturn(Optional.of(ticket));

        Showtime showtime = new Showtime();
        showtime.setShowtimeId(2L);
        showtime.setStartTime(START);
        showtime.setEndTime(START.plusHours(2));
        showtime.setMovieId(5L);
        showtime.setRoomId(6L);
        showtime.setStatus(ShowtimeStatus.AVAILABLE);
        showtime.setIsDeleted(false);
        when(showtimes.getById(2L)).thenReturn(showtime);
    }

    @ParameterizedTest(name = "now = start + {0} seconds")
    @ValueSource(longs = {-1800, 7199})
    @DisplayName("UT-16 / TKT-08: check-in accepts exactly start-30m and end-1s")
    void checkInAcceptsInclusiveOpeningAndLastSecond(long secondsFromStart) {
        stubResponseDetails();

        CheckInResponseDTO response = serviceAt(START.plusSeconds(secondsFromStart)).checkIn(request);

        assertEquals(Long.valueOf(1L), response.getTicketId());
        assertTrue(ticket.getIsCheckedIn());
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        assertEquals(FoodFulfillmentStatus.FULFILLED, booking.getFoodFulfillmentStatus());
        assertTrue(response.getFoodDeliveredNow());
        verify(tickets).save(ticket);
        verify(bookings).save(booking);
    }

    @ParameterizedTest(name = "now = start + {0} seconds")
    @CsvSource({
            "-1801, Chưa đến giờ check-in (chỉ được check-in trước giờ chiếu tối đa 30 phút)",
            "7200, Suất chiếu của vé này đã kết thúc",
            "7201, Suất chiếu của vé này đã kết thúc"
    })
    @DisplayName("UT-16 / TKT-08: check-in rejects start-30m-1s, exact end and end+1s")
    void checkInRejectsOutsideWindowWithoutMutation(long secondsFromStart, String expectedMessage) {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> serviceAt(START.plusSeconds(secondsFromStart)).checkIn(request));

        assertEquals(ErrorCode.VALIDATION_FAILED, exception.getCode());
        assertEquals(expectedMessage, exception.getMessage());
        assertFalse(ticket.getIsCheckedIn());
        assertEquals(BookingStatus.PAID, booking.getStatus());
        assertEquals(FoodFulfillmentStatus.PENDING, booking.getFoodFulfillmentStatus());
        verify(tickets, never()).save(any());
        verify(bookings, never()).save(any());
        verifyNoInteractions(movies, rooms, cinemas, seats);
    }

    @Test
    @DisplayName("UT-16: the same QR succeeds once, then is rejected without a second save")
    void checkInRejectsSecondScanOfSameTicket() {
        stubResponseDetails();
        TicketCheckInService service = serviceAt(START.minusMinutes(30));
        assertNotNull(service.checkIn(request));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.checkIn(request));

        assertEquals(ErrorCode.TICKET_ALREADY_USED, exception.getCode());
        assertTrue(ticket.getIsCheckedIn());
        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        assertEquals(FoodFulfillmentStatus.FULFILLED, booking.getFoodFulfillmentStatus());
        verify(tickets, times(1)).save(ticket);
        verify(bookings, times(1)).save(booking);
    }

    private TicketCheckInService serviceAt(LocalDateTime now) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Clock clock = Clock.fixed(now.atZone(zone).toInstant(), zone);
        return new TicketCheckInService(tickets, showtimes, movies, rooms, cinemas, seats,
                bookings, jwt, null, clock);
    }

    private void stubResponseDetails() {
        Movie movie = new Movie();
        movie.setTitle("Boundary test movie");
        when(movies.getById(5L)).thenReturn(movie);
        Room room = new Room();
        room.setRoomName("Room 1");
        room.setCinemaId(7L);
        when(rooms.findById(6L)).thenReturn(room);
        Cinema cinema = new Cinema();
        cinema.setName("Test cinema");
        when(cinemas.findById(7L)).thenReturn(cinema);
        Seat seat = new Seat();
        seat.setSeatRow(BigDecimal.ONE);
        seat.setSeatCol(BigDecimal.ONE);
        when(seats.findById(4L)).thenReturn(Optional.of(seat));
    }
}
