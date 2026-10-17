package com.uit.backend_cinema.unit_test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.auth.domain.entity.AccountType;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.entity.UserRank;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.booking.api.dto.CreateStaffBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.api.dto.CustomerLookupResponseDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingSource;
import com.uit.backend_cinema.modules.booking.domain.entity.CustomerType;
import com.uit.backend_cinema.modules.booking.domain.entity.PrePaymentBookingQuote;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.PaymentConfirmationRepository;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.service.PriceConfigService;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.entity.ShowtimeStatus;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffBookingAndWalkInTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private SeatService seatService;
    @Mock private ShowtimeService showtimeService;
    @Mock private PriceConfigService priceConfigService;
    @Mock private TicketService ticketService;
    @Mock private FoodOrderService foodOrderService;
    @Mock private VoucherService voucherService;
    @Mock private PaymentConfirmationRepository paymentConfirmationRepository;
    @Mock private PaymentReceiptRepository paymentReceiptRepository;
    @Mock private OutboxEventService outboxEventService;
    @Mock private ObjectMapper objectMapper;
    @Mock private UserRepository userRepository;
    @Mock private Clock clock;

    private BookingService bookingService;
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 17, 10, 0);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(now.toInstant(ZoneOffset.UTC));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        bookingService = new BookingService(
                bookingRepository, seatService, showtimeService, priceConfigService,
                ticketService, foodOrderService, voucherService, paymentConfirmationRepository,
                paymentReceiptRepository, outboxEventService, objectMapper, userRepository, clock);
    }

    private Showtime createShowtime(Long showtimeId) {
        Showtime s = new Showtime();
        s.setShowtimeId(showtimeId);
        s.setRoomId(1L);
        s.setFormat(MovieFormat.TYPE_2D);
        s.setStartTime(now.plusHours(1));
        s.setEndTime(now.plusHours(3));
        s.setBasePrice(new BigDecimal("100000"));
        s.setStatus(ShowtimeStatus.AVAILABLE);
        return s;
    }

    private Seat createSeat(Long seatId) {
        Seat seat = new Seat();
        seat.setSeatId(seatId);
        seat.setRoomId(1L);
        SeatType st = new SeatType();
        st.setPriceMultiplier(BigDecimal.ONE);
        st.setTypeName("NORMAL");
        seat.setSeatType(st);
        return seat;
    }

    @Test
    @DisplayName("Staff tạo booking cho WALK_IN: giá không giảm hạng, source COUNTER, lưu thông tin liên hệ")
    void testStaffCreatesWalkInBookingNoDiscount() {
        Long staffId = 99L;
        Long showtimeId = 1L;

        User staff = new User();
        staff.setUserId(staffId);
        staff.setRole("STAFF");
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));

        User walkInUser = new User();
        walkInUser.setUserId(2L);
        walkInUser.setName("Khách vãng lai");
        walkInUser.setAccountType(AccountType.WALK_IN);
        walkInUser.setRankLevel(UserRank.GOLD); // Even if rank set, WALK_IN must get 0% discount
        when(userRepository.getOrCreateWalkInUser()).thenReturn(walkInUser);

        Showtime showtime = createShowtime(showtimeId);
        when(showtimeService.getByIdForUpdate(showtimeId)).thenReturn(showtime);
        when(seatService.promoteLocksForCheckout(eq(showtimeId), any(), eq(1L), eq(staffId)))
                .thenReturn(List.of(createSeat(10L)));
        when(priceConfigService.getPriceMultiplier(any(), any())).thenReturn(BigDecimal.ONE);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(foodOrderService.createDraftItems(any(), any())).thenReturn(new ArrayList<>());
        when(foodOrderService.calculateDraftSubtotal(any())).thenReturn(BigDecimal.ZERO);

        CreateStaffBookingRequestDTO request = new CreateStaffBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setCustomerType(CustomerType.WALK_IN);
        request.setPaymentMethod(BookingPaymentMethod.CASH);
        request.setSeatIds(List.of(10L));
        request.setContactName("Nguyen Van A");
        request.setContactPhone("0901234567");

        PrePaymentBookingQuote quote = bookingService.createStaffBooking(staffId, request);

        assertNotNull(quote);
        assertEquals(0, new BigDecimal("100000").compareTo(quote.getTotalAmount()));
        assertEquals(0, new BigDecimal("100000").compareTo(quote.getSeatSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(quote.getDiscountAmount()));
    }

    @Test
    @DisplayName("Staff tạo booking cho REGISTERED: khách hưởng giảm giá theo hạng")
    void testStaffCreatesRegisteredBookingWithRankDiscount() {
        Long staffId = 99L;
        Long customerId = 55L;
        Long showtimeId = 1L;

        User staff = new User();
        staff.setUserId(staffId);
        staff.setRole("STAFF");
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));

        User customer = new User();
        customer.setUserId(customerId);
        customer.setName("Thành viên VIP");
        customer.setEmail("vip@example.com");
        customer.setAccountType(AccountType.REGISTERED);
        customer.setRankLevel(UserRank.GOLD); // 3% discount
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Showtime showtime = createShowtime(showtimeId);
        when(showtimeService.getByIdForUpdate(showtimeId)).thenReturn(showtime);
        when(seatService.promoteLocksForCheckout(eq(showtimeId), any(), eq(1L), eq(staffId)))
                .thenReturn(List.of(createSeat(10L)));
        when(priceConfigService.getPriceMultiplier(any(), any())).thenReturn(BigDecimal.ONE);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(foodOrderService.createDraftItems(any(), any())).thenReturn(new ArrayList<>());
        when(foodOrderService.calculateDraftSubtotal(any())).thenReturn(BigDecimal.ZERO);

        CreateStaffBookingRequestDTO request = new CreateStaffBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setCustomerType(CustomerType.REGISTERED);
        request.setCustomerId(customerId);
        request.setPaymentMethod(BookingPaymentMethod.CASH);
        request.setSeatIds(List.of(10L));

        PrePaymentBookingQuote quote = bookingService.createStaffBooking(staffId, request);

        assertNotNull(quote);
        // 100,000 * (1 - 0.03) = 97,000
        assertEquals(0, new BigDecimal("97000").compareTo(quote.getTotalAmount()));
        assertEquals(0, new BigDecimal("97000").compareTo(quote.getSeatSubtotal()));
    }

    @Test
    @DisplayName("Staff tạo booking REGISTERED thiếu customerId ném VALIDATION_FAILED")
    void testStaffCreatesRegisteredBookingMissingCustomerIdThrows() {
        Long staffId = 99L;
        User staff = new User();
        staff.setUserId(staffId);
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));

        CreateStaffBookingRequestDTO request = new CreateStaffBookingRequestDTO();
        request.setShowtimeId(1L);
        request.setCustomerType(CustomerType.REGISTERED);
        request.setCustomerId(null);
        request.setPaymentMethod(BookingPaymentMethod.CASH);
        request.setSeatIds(List.of(10L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.createStaffBooking(staffId, request));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getCode());
    }

    @Test
    @DisplayName("Tra cứu khách hàng thành viên theo email trả về thông tin hợp lệ")
    void testLookupCustomerByEmailSuccess() {
        String email = "customer@example.com";
        User user = new User();
        user.setUserId(10L);
        user.setName("Khách hàng A");
        user.setEmail(email);
        user.setPhone("0912345678");
        user.setRankLevel(UserRank.SILVER);
        user.setAccountType(AccountType.REGISTERED);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        CustomerLookupResponseDTO result = bookingService.lookupCustomerByEmail(email);

        assertNotNull(result);
        assertEquals(10L, result.getUserId());
        assertEquals(email, result.getEmail());
        assertEquals("SILVER", result.getRankLevel());
    }

    @Test
    @DisplayName("Tra cứu tài khoản WALK_IN hoặc đã xóa bị từ chối")
    void testLookupWalkInAccountRejected() {
        String email = "walkin@cinema.internal";
        User user = new User();
        user.setUserId(2L);
        user.setEmail(email);
        user.setAccountType(AccountType.WALK_IN);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> bookingService.lookupCustomerByEmail(email));
    }
}
