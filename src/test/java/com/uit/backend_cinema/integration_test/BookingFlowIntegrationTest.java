package com.uit.backend_cinema.integration_test;

import com.uit.backend_cinema.modules.auth.infrastructure.entity.RoleJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.UserJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaRoleRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaUserRepository;
import com.uit.backend_cinema.modules.booking.api.dto.CreateBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.PrePaymentBookingQuote;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingJpaEntity;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaBookingRepository;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaPaymentConfirmationRepository;
import com.uit.backend_cinema.modules.cinema.infrastructure.entity.RoomJpaEntity;
import com.uit.backend_cinema.modules.cinema.infrastructure.repository.JpaRoomRepository;
import com.uit.backend_cinema.modules.notification.domain.repository.OtpStorage;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.infrastructure.entity.OutboxEventJpaEntity;
import com.uit.backend_cinema.modules.outbox.infrastructure.repository.JpaOutboxEventRepository;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatStatus;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;
import com.uit.backend_cinema.modules.seat.infrastructure.entity.SeatJpaEntity;
import com.uit.backend_cinema.modules.seat.infrastructure.entity.SeatTypeJpaEntity;
import com.uit.backend_cinema.modules.seat.infrastructure.repository.JpaSeatRepository;
import com.uit.backend_cinema.modules.seat.infrastructure.repository.JpaSeatTypeRepository;
import com.uit.backend_cinema.modules.showtime.domain.entity.ShowtimeStatus;
import com.uit.backend_cinema.modules.showtime.infrastructure.entity.ShowtimeJpaEntity;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.JpaShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
public class BookingFlowIntegrationTest extends TestcontainersConfig {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JpaBookingRepository jpaBookingRepository;

    @Autowired
    private JpaPaymentConfirmationRepository jpaPaymentConfirmationRepository;

    @Autowired
    private JpaOutboxEventRepository jpaOutboxEventRepository;

    @Autowired
    private JpaUserRepository jpaUserRepository;

    @Autowired
    private JpaRoleRepository jpaRoleRepository;

    @Autowired
    private JpaRoomRepository jpaRoomRepository;

    @Autowired
    private JpaSeatRepository jpaSeatRepository;

    @Autowired
    private JpaSeatTypeRepository jpaSeatTypeRepository;

    @Autowired
    private JpaShowtimeRepository jpaShowtimeRepository;

    @MockBean
    private SeatLockRepository seatLockRepository;

    @MockBean
    private OtpStorage otpStorage;

    private Long userId;
    private Long roomId;
    private Long seatId;
    private Long seatTypeId;
    private Long showtimeId;

    @BeforeEach
    void cleanAndSeed() {
        jpaBookingRepository.deleteAllInBatch();
        jpaPaymentConfirmationRepository.deleteAllInBatch();
        jpaOutboxEventRepository.deleteAllInBatch();
        jpaSeatRepository.deleteAllInBatch();
        jpaSeatTypeRepository.deleteAllInBatch();
        jpaShowtimeRepository.deleteAllInBatch();
        jpaRoomRepository.deleteAllInBatch();
        jpaUserRepository.deleteAllInBatch();
        jpaRoleRepository.deleteAllInBatch();

        // Seed Role
        RoleJpaEntity role = new RoleJpaEntity();
        role.setRoleName("ROLE_USER");
        jpaRoleRepository.save(role);

        // Seed User
        UserJpaEntity user = new UserJpaEntity();
        user.setName("John Doe");
        user.setEmail("johndoe@example.com");
        user.setPassword("password");
        user.setRankLevel("SILVER");
        user.setTotalPaid(BigDecimal.ZERO);
        user.setTotalPoint(0);
        user.setAuthProvider("EMAIL");
        user.setIsDeleted(false);
        user.setRoles(Set.of(role));
        user = jpaUserRepository.save(user);
        this.userId = user.getUserId();

        // Seed Room
        RoomJpaEntity room = new RoomJpaEntity();
        room.setCinemaId(1L);
        room.setRoomName("Room 1");
        room.setDeleted(false);
        room = jpaRoomRepository.save(room);
        this.roomId = room.getRoomId();

        // Seed Seat Type
        SeatTypeJpaEntity seatType = new SeatTypeJpaEntity();
        seatType.setTypeName("STANDARD");
        seatType.setPriceMultiplier(BigDecimal.ONE);
        seatType = jpaSeatTypeRepository.save(seatType);
        this.seatTypeId = seatType.getSeatTypeId();

        // Seed Seat
        SeatJpaEntity seat = new SeatJpaEntity();
        seat.setRoomId(roomId);
        seat.setSeatRow(BigDecimal.ONE);
        seat.setSeatCol(BigDecimal.ONE);
        seat.setSeatType(seatType);
        seat.setStatus(SeatStatus.ACTIVE);
        seat.setIsDeleted(false);
        seat = jpaSeatRepository.save(seat);
        this.seatId = seat.getSeatId();

        // Seed Showtime
        ShowtimeJpaEntity showtime = new ShowtimeJpaEntity();
        showtime.setRoomId(roomId);
        showtime.setMovieId(999L);
        showtime.setFormat(MovieFormat.TYPE_2D);
        showtime.setStatus(ShowtimeStatus.AVAILABLE);
        showtime.setStartTime(LocalDateTime.now().plusDays(1));
        showtime.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        showtime.setBasePrice(new BigDecimal("100000"));
        showtime.setIsDeleted(false);
        showtime = jpaShowtimeRepository.save(showtime);
        this.showtimeId = showtime.getShowtimeId();

        // Mock Seat Lock behaviors
        when(seatLockRepository.promoteLockIfOwner(any(), any(), any(), anyLong())).thenReturn(true);
        when(seatLockRepository.getLockedBy(any(), any())).thenReturn(String.valueOf(userId));
    }

    @Test
    @DisplayName("1. Tạo Booking Chờ Thanh Toán -> Lưu DB Trạng Thái PENDING")
    void testCreateBookingFlow() {
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Collections.singletonList(seatId));
        request.setPaymentMethod(BookingPaymentMethod.MOMO);

        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        assertNotNull(quote);
        assertNotNull(quote.getBookingId());

        Optional<BookingJpaEntity> dbBooking = jpaBookingRepository.findById(quote.getBookingId());
        assertTrue(dbBooking.isPresent());
        assertEquals(BookingStatus.PENDING, dbBooking.get().getStatus());
        assertEquals(userId, dbBooking.get().getUserId());
    }

    @Test
    @DisplayName("2. Xác Nhận Thanh Toán -> Chuyển Trạng Thái Sang PAID")
    void testConfirmPaymentFlow() {
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Collections.singletonList(seatId));
        request.setPaymentMethod(BookingPaymentMethod.MOMO);
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        bookingService.confirmPaymentByGateway(quote.getBookingId(), "momo-ref-12345", "MOMO");

        Optional<BookingJpaEntity> dbBooking = jpaBookingRepository.findById(quote.getBookingId());
        assertTrue(dbBooking.isPresent());
        assertEquals(BookingStatus.PAID, dbBooking.get().getStatus());

        var confirmations = jpaPaymentConfirmationRepository.findAll();
        assertEquals(1, confirmations.size());
        assertEquals("momo-ref-12345", confirmations.get(0).getPaymentRef());
    }

    @Test
    @DisplayName("3. Tạo Outbox Event Sau Khi Thanh Toán Thành Công")
    void testOutboxEventCreationFlow() {
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Collections.singletonList(seatId));
        request.setPaymentMethod(BookingPaymentMethod.MOMO);
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        bookingService.confirmPaymentByGateway(quote.getBookingId(), "momo-ref-9999", "MOMO");

        List<OutboxEventJpaEntity> events = jpaOutboxEventRepository.findAll();
        assertFalse(events.isEmpty());

        boolean hasBookingPaidEvent = events.stream()
                .anyMatch(e -> e.getEventType() == OutboxEventType.BOOKING_PAID &&
                        e.getAggregateId().equals(quote.getBookingId().toString()));
        assertTrue(hasBookingPaidEvent);
    }

    @Test
    @DisplayName("4. Dọn Dẹp Booking Hết Hạn -> Chuyển Trạng Thái PENDING Sang EXPIRED")
    void testExpiredBookingCleanupFlow() {
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Collections.singletonList(seatId));
        request.setPaymentMethod(BookingPaymentMethod.MOMO);
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        // Giả lập booking hết hạn bằng cách cập nhật expires_at lùi về quá khứ 1 tiếng
        BookingJpaEntity jpaBooking = jpaBookingRepository.findById(quote.getBookingId()).orElseThrow();
        jpaBooking.setExpiresAt(LocalDateTime.now().minusHours(1));
        jpaBookingRepository.saveAndFlush(jpaBooking);

        // Kích hoạt dọn dẹp booking hết hạn
        bookingService.expirePendingBookings();

        Optional<BookingJpaEntity> dbBooking = jpaBookingRepository.findById(quote.getBookingId());
        assertTrue(dbBooking.isPresent());
        assertEquals(BookingStatus.EXPIRED, dbBooking.get().getStatus());
    }

    @Test
    @DisplayName("5. Kiểm Tra Khóa Lạc Quan (Optimistic Lock) Khi Cập Nhật Booking Trùng Phiên Bản")
    void testOptimisticLockingOnBooking() {
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Collections.singletonList(seatId));
        request.setPaymentMethod(BookingPaymentMethod.MOMO);
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        // Fetch cùng một booking ở 2 đối tượng domain khác nhau (có cùng phiên bản version ban đầu)
        Booking booking1 = bookingRepository.findById(quote.getBookingId()).orElseThrow();
        Booking booking2 = bookingRepository.findById(quote.getBookingId()).orElseThrow();

        assertNotNull(booking1.getVersion());
        assertEquals(booking1.getVersion(), booking2.getVersion());

        // Thay đổi và lưu đối tượng thứ nhất -> tăng version lên
        booking1.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking1);

        // Thay đổi và lưu đối tượng thứ hai (phiên bản version trong tay vẫn là 0)
        // Lưu lần này phải ném ra lỗi khóa lạc quan ObjectOptimisticLockingFailureException
        booking2.setStatus(BookingStatus.EXPIRED);
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            bookingRepository.save(booking2);
        });
    }
}
