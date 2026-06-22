package com.uit.backend_cinema.modules.ticket.domain.service;

import java.time.LocalDateTime;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class TicketCheckInService {

    private final TicketRepository ticketRepository;
    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final RoomService roomService;
    private final CinemaService cinemaService;
    private final SeatRepository seatRepository;
    private final JwtUtil jwtUtil;

    public TicketCheckInService(TicketRepository ticketRepository,
                                ShowtimeService showtimeService,
                                MovieService movieService,
                                RoomService roomService,
                                CinemaService cinemaService,
                                SeatRepository seatRepository,
                                JwtUtil jwtUtil) {
        this.ticketRepository = ticketRepository;
        this.showtimeService = showtimeService;
        this.movieService = movieService;
        this.roomService = roomService;
        this.cinemaService = cinemaService;
        this.seatRepository = seatRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public CheckInResponseDTO checkIn(CheckInRequestDTO request) {
        Claims claims = jwtUtil.validateTicketQrToken(request.getQrToken());
        if (claims == null) {
            throw new BusinessException("Mã QR vé không hợp lệ hoặc đã hết hạn", ErrorCode.TICKET_INVALID_QR);
        }

        Number ticketIdNum = claims.get("ticketId", Number.class);
        Number showtimeIdNum = claims.get("showtimeId", Number.class);
        Number bookingIdNum = claims.get("bookingId", Number.class);

        if (ticketIdNum == null || showtimeIdNum == null || bookingIdNum == null) {
            throw new BusinessException("Thông tin vé trong QR không đầy đủ", ErrorCode.TICKET_INVALID_QR);
        }

        Long ticketId = ticketIdNum.longValue();
        Long showtimeId = showtimeIdNum.longValue();
        Long bookingId = bookingIdNum.longValue();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin vé trong hệ thống", ErrorCode.RESOURCE_NOT_FOUND));

        if (!ticket.getBookingId().equals(bookingId) || !ticket.getShowtimeId().equals(showtimeId)) {
            throw new BusinessException("Thông tin vé không khớp với mã QR đã quét", ErrorCode.TICKET_INVALID_QR);
        }

        if (Boolean.TRUE.equals(ticket.getIsCheckedIn())) {
            throw new BusinessException("Vé này đã được check-in trước đó", ErrorCode.TICKET_ALREADY_USED);
        }

        Showtime showtime = showtimeService.getById(showtimeId);
        LocalDateTime now = LocalDateTime.now();
        if (showtime.getStartTime().isAfter(now.plusDays(1))) {
            throw new BusinessException("Chưa đến ngày chiếu, chỉ được check-in trước tối đa 24 giờ", ErrorCode.VALIDATION_FAILED);
        }
        if (showtime.getEndTime().isBefore(now.minusHours(4))) {
            throw new BusinessException("Suất chiếu của vé này đã kết thúc", ErrorCode.VALIDATION_FAILED);
        }

        // Cập nhật trạng thái
        ticket.setIsCheckedIn(true);
        ticketRepository.save(ticket);

        // Lấy chi tiết thông tin
        Movie movie = movieService.getById(showtime.getMovieId());
        Room room = roomService.findById(showtime.getRoomId());
        Cinema cinema = cinemaService.findById(room.getCinemaId());
        Seat seat = seatRepository.findById(ticket.getSeatId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin ghế", ErrorCode.RESOURCE_NOT_FOUND));

        String seatName = getSeatName(seat);

        return CheckInResponseDTO.builder()
                .ticketId(ticketId)
                .bookingId(bookingId)
                .movieTitle(movie.getTitle())
                .cinemaName(cinema.getName())
                .roomName(room.getRoomName())
                .showtimeStart(showtime.getStartTime())
                .seatName(seatName)
                .build();
    }

    private String getSeatName(Seat seat) {
        if (seat == null) return "N/A";
        char rowLetter = (char) ('A' + seat.getSeatRow().intValue() - 1);
        return rowLetter + String.valueOf(seat.getSeatCol().intValue());
    }
}
