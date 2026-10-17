package com.uit.backend_cinema.modules.ticket.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.entity.ShowtimeStatus;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInFoodItemDTO;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInRequestDTO;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInResponseDTO;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.entity.TicketStatus;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;

@Service
public class TicketCheckInService {

    private final TicketRepository ticketRepository;
    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final RoomService roomService;
    private final CinemaService cinemaService;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final JwtUtil jwtUtil;
    private final FoodOrderService foodOrderService;
    private final Clock clock;

    @Autowired
    public TicketCheckInService(TicketRepository ticketRepository,
                                ShowtimeService showtimeService,
                                MovieService movieService,
                                RoomService roomService,
                                CinemaService cinemaService,
                                SeatRepository seatRepository,
                                BookingRepository bookingRepository,
                                JwtUtil jwtUtil,
                                @Autowired(required = false) FoodOrderService foodOrderService,
                                Clock clock) {
        this.ticketRepository = ticketRepository;
        this.showtimeService = showtimeService;
        this.movieService = movieService;
        this.roomService = roomService;
        this.cinemaService = cinemaService;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.jwtUtil = jwtUtil;
        this.foodOrderService = foodOrderService;
        this.clock = clock != null ? clock : Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public TicketCheckInService(TicketRepository ticketRepository,
                                ShowtimeService showtimeService,
                                MovieService movieService,
                                RoomService roomService,
                                CinemaService cinemaService,
                                SeatRepository seatRepository,
                                BookingRepository bookingRepository,
                                JwtUtil jwtUtil) {
        this(ticketRepository, showtimeService, movieService, roomService,
             cinemaService, seatRepository, bookingRepository, jwtUtil, null, null);
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

        // 1. Khóa booking trước ticket và fulfillment để tuần tự hóa các yêu cầu đồng thời
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseGet(() -> bookingRepository.findById(bookingId)
                        .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND)));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REFUNDED
                || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BusinessException("Đơn hàng không ở trạng thái hợp lệ để check-in (" + booking.getStatus() + ")",
                    ErrorCode.VALIDATION_FAILED);
        }

        // 2. Kiểm tra thông tin vé
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy thông tin vé trong hệ thống", ErrorCode.RESOURCE_NOT_FOUND));

        if (!ticket.getBookingId().equals(bookingId) || !ticket.getShowtimeId().equals(showtimeId)) {
            throw new BusinessException("Thông tin vé không khớp với mã QR đã quét", ErrorCode.TICKET_INVALID_QR);
        }

        if (ticket.getStatus() != TicketStatus.VALID) {
            throw new BusinessException("Vé này không hợp lệ hoặc đã bị hủy", ErrorCode.VALIDATION_FAILED);
        }

        if (Boolean.TRUE.equals(ticket.getIsCheckedIn())) {
            throw new BusinessException("Vé này đã được check-in trước đó", ErrorCode.TICKET_ALREADY_USED);
        }

        // 3. Kiểm tra suất chiếu và biên thời gian (start - 30m <= now < end)
        Showtime showtime = showtimeService.getById(showtimeId);
        if (showtime.getStatus() == ShowtimeStatus.CANCELLED || Boolean.TRUE.equals(showtime.getIsDeleted())) {
            throw new BusinessException("Suất chiếu của vé này đã bị hủy", ErrorCode.VALIDATION_FAILED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(showtime.getStartTime().minusMinutes(30))) {
            throw new BusinessException("Chưa đến giờ check-in (chỉ được check-in trước giờ chiếu tối đa 30 phút)",
                    ErrorCode.VALIDATION_FAILED);
        }
        if (showtime.getEndTime() != null && !now.isBefore(showtime.getEndTime())) {
            throw new BusinessException("Suất chiếu của vé này đã kết thúc", ErrorCode.VALIDATION_FAILED);
        }

        // 4. Cập nhật trạng thái vé
        ticket.setIsCheckedIn(true);
        ticketRepository.save(ticket);

        // 5. Cập nhật booking sang CHECKED_IN và giao đồ ăn một lần duy nhất
        if (booking.getStatus() == BookingStatus.PAID) {
            booking.setStatus(BookingStatus.CHECKED_IN);
        }

        boolean foodDeliveredNow = false;
        if (booking.getFoodFulfillmentStatus() == FoodFulfillmentStatus.PENDING) {
            booking.setFoodFulfillmentStatus(FoodFulfillmentStatus.FULFILLED);
            foodDeliveredNow = true;
        }
        bookingRepository.save(booking);

        // 6. Lấy danh sách món ăn từ đơn hàng (nếu có)
        List<CheckInFoodItemDTO> foodItems = List.of();
        if (foodOrderService != null) {
            foodItems = foodOrderService.getByBookingId(bookingId)
                    .map(order -> order.getItems().stream()
                            .map(item -> CheckInFoodItemDTO.builder()
                                    .foodId(item.getFoodId())
                                    .foodName(item.getFoodName())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .toList())
                    .orElse(List.of());
        }

        // 7. Lấy chi tiết thông tin phim, rạp, phòng, ghế
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
                .foodFulfillmentStatus(booking.getFoodFulfillmentStatus())
                .foodDeliveredNow(foodDeliveredNow)
                .foodItems(foodItems)
                .build();
    }

    private String getSeatName(Seat seat) {
        if (seat == null) return "N/A";
        char rowLetter = (char) ('A' + seat.getSeatRow().intValue() - 1);
        return rowLetter + String.valueOf(seat.getSeatCol().intValue());
    }
}
