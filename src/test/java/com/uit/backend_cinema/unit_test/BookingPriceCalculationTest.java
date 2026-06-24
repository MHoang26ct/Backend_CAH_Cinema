package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.entity.UserRank;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.booking.api.dto.CreateBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.PrePaymentBookingQuote;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.PaymentConfirmationRepository;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.service.PriceConfigService;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPriceCalculationTest {

    @InjectMocks
    private BookingService bookingService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SeatService seatService;

    @Mock
    private ShowtimeService showtimeService;

    @Mock
    private PriceConfigService priceConfigService;

    @Mock
    private TicketService ticketService;

    @Mock
    private FoodOrderService foodOrderService;

    @Mock
    private VoucherService voucherService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentConfirmationRepository paymentConfirmationRepository;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private ObjectMapper objectMapper;

    private Seat createSeat(Long id, BigDecimal priceMultiplier, String typeName) {
        Seat seat = new Seat();
        seat.setSeatId(id);
        seat.setRoomId(1L);
        SeatType type = new SeatType();
        type.setSeatTypeId(typeName.hashCode() * 1L);
        type.setTypeName(typeName);
        type.setPriceMultiplier(priceMultiplier);
        seat.setSeatType(type);
        return seat;
    }

    private Showtime createShowtime(Long id, BigDecimal basePrice) {
        Showtime showtime = new Showtime();
        showtime.setShowtimeId(id);
        showtime.setRoomId(1L);
        showtime.setBasePrice(basePrice);
        showtime.setStartTime(LocalDateTime.now().plusDays(1));
        showtime.setFormat(MovieFormat.TYPE_2D);
        return showtime;
    }

    private User createUser(Long id, UserRank rank) {
        User user = new User();
        user.setUserId(id);
        user.setRankLevel(rank);
        return user;
    }

    @Test
    @DisplayName("1. Hạng SILVER (giảm 2%), không voucher, 1 ghế Standard")
    void testSilverRankNoVoucherSingleSeat() {
        Long userId = 1L;
        Long showtimeId = 1L;
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Collections.singletonList(10L));
        request.setPaymentMethod(BookingPaymentMethod.MOMO);

        Showtime showtime = createShowtime(showtimeId, new BigDecimal("100000"));
        Seat seat = createSeat(10L, new BigDecimal("1.00"), "STANDARD");
        User user = createUser(userId, UserRank.SILVER);

        when(showtimeService.getById(showtimeId)).thenReturn(showtime);
        when(seatService.promoteLocksForCheckout(eq(showtimeId), any(), eq(1L), eq(userId)))
                .thenReturn(Collections.singletonList(seat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(priceConfigService.getPriceMultiplier(any(), any())).thenReturn(new BigDecimal("1.20")); // Showtime multiplier (e.g. evening)
        
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(foodOrderService.createDraftItems(any(), any())).thenReturn(new ArrayList<>());
        when(foodOrderService.calculateDraftSubtotal(any())).thenReturn(BigDecimal.ZERO);
        when(voucherService.applyVoucherForBooking(any(), any())).thenReturn(BigDecimal.ZERO);

        // Seat subtotal: 1.00 * 100000 * 1.20 = 120000
        // Silver discount: 2% -> factor = 0.98 -> 120000 * 0.98 = 117600
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        assertNotNull(quote);
        assertEquals(0, quote.getSeatSubtotal().compareTo(new BigDecimal("117600")));
        assertEquals(0, quote.getFoodSubtotal().compareTo(BigDecimal.ZERO));
        assertEquals(0, quote.getDiscountAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, quote.getTotalAmount().compareTo(new BigDecimal("117600")));
    }

    @Test
    @DisplayName("2. Hạng GOLD (giảm 3%), có voucher giảm giá, 1 ghế VIP")
    void testGoldRankWithVoucherSingleSeat() {
        Long userId = 1L;
        Long showtimeId = 1L;
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Collections.singletonList(10L));
        request.setVoucherId(99L);
        request.setPaymentMethod(BookingPaymentMethod.VNPAY);

        Showtime showtime = createShowtime(showtimeId, new BigDecimal("100000"));
        Seat seat = createSeat(10L, new BigDecimal("1.20"), "VIP");
        User user = createUser(userId, UserRank.GOLD);

        when(showtimeService.getById(showtimeId)).thenReturn(showtime);
        when(seatService.promoteLocksForCheckout(eq(showtimeId), any(), eq(1L), eq(userId)))
                .thenReturn(Collections.singletonList(seat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(priceConfigService.getPriceMultiplier(any(), any())).thenReturn(new BigDecimal("1.00"));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(foodOrderService.createDraftItems(any(), any())).thenReturn(new ArrayList<>());
        when(foodOrderService.calculateDraftSubtotal(any())).thenReturn(BigDecimal.ZERO);
        // Voucher giảm 10000
        when(voucherService.applyVoucherForBooking(eq(99L), any())).thenReturn(new BigDecimal("10000"));

        // Seat subtotal: 1.20 * 100000 * 1.00 = 120000
        // Gold discount: 3% -> factor = 0.97 -> 120000 * 0.97 = 116400
        // Voucher discount: 10000
        // Total: 116400 - 10000 = 106400
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        assertNotNull(quote);
        assertEquals(0, quote.getSeatSubtotal().compareTo(new BigDecimal("116400")));
        assertEquals(0, quote.getDiscountAmount().compareTo(new BigDecimal("10000")));
        assertEquals(0, quote.getTotalAmount().compareTo(new BigDecimal("106400")));
    }

    @Test
    @DisplayName("3. Hạng DIAMOND (giảm 5%), kèm đồ ăn, có voucher")
    void testDiamondRankWithFoodAndVoucher() {
        Long userId = 1L;
        Long showtimeId = 1L;
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Collections.singletonList(10L));
        request.setVoucherId(99L);
        request.setPaymentMethod(BookingPaymentMethod.MOMO);

        Showtime showtime = createShowtime(showtimeId, new BigDecimal("80000"));
        Seat seat = createSeat(10L, new BigDecimal("1.00"), "STANDARD");
        User user = createUser(userId, UserRank.DIAMOND);

        when(showtimeService.getById(showtimeId)).thenReturn(showtime);
        when(seatService.promoteLocksForCheckout(eq(showtimeId), any(), eq(1L), eq(userId)))
                .thenReturn(Collections.singletonList(seat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(priceConfigService.getPriceMultiplier(any(), any())).thenReturn(new BigDecimal("1.00"));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(foodOrderService.createDraftItems(any(), any())).thenReturn(new ArrayList<>());
        when(foodOrderService.calculateDraftSubtotal(any())).thenReturn(new BigDecimal("50000")); // Food subtotal = 50000
        when(voucherService.applyVoucherForBooking(eq(99L), any())).thenReturn(new BigDecimal("20000"));

        // Seat subtotal after rank: 80000 * 0.95 = 76000
        // Food subtotal after rank: 50000 * 0.95 = 47500
        // Subtotal after rank: 76000 + 47500 = 123500
        // Voucher discount: 20000
        // Total: 123500 - 20000 = 103500
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        assertNotNull(quote);
        assertEquals(0, quote.getSeatSubtotal().compareTo(new BigDecimal("76000")));
        assertEquals(0, quote.getFoodSubtotal().compareTo(new BigDecimal("47500")));
        assertEquals(0, quote.getDiscountAmount().compareTo(new BigDecimal("20000")));
        assertEquals(0, quote.getTotalAmount().compareTo(new BigDecimal("103500")));
    }

    @Test
    @DisplayName("4. Voucher lớn hơn tổng tiền -> Tổng tiền bằng 0")
    void testVoucherExceedsSubtotal() {
        Long userId = 1L;
        Long showtimeId = 1L;
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Collections.singletonList(10L));
        request.setVoucherId(99L);
        request.setPaymentMethod(BookingPaymentMethod.MOMO);

        Showtime showtime = createShowtime(showtimeId, new BigDecimal("50000"));
        Seat seat = createSeat(10L, new BigDecimal("1.00"), "STANDARD");
        User user = createUser(userId, UserRank.SILVER);

        when(showtimeService.getById(showtimeId)).thenReturn(showtime);
        when(seatService.promoteLocksForCheckout(eq(showtimeId), any(), eq(1L), eq(userId)))
                .thenReturn(Collections.singletonList(seat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(priceConfigService.getPriceMultiplier(any(), any())).thenReturn(new BigDecimal("1.00"));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(foodOrderService.createDraftItems(any(), any())).thenReturn(new ArrayList<>());
        when(foodOrderService.calculateDraftSubtotal(any())).thenReturn(BigDecimal.ZERO);
        // Voucher discount is 60000 (exceeds subtotal)
        when(voucherService.applyVoucherForBooking(eq(99L), any())).thenReturn(new BigDecimal("60000"));

        // Seat subtotal after rank: 50000 * 0.98 = 49000
        // Total amount: max(0, 49000 - 60000) = 0
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        assertNotNull(quote);
        assertEquals(0, quote.getSeatSubtotal().compareTo(new BigDecimal("49000")));
        assertEquals(0, quote.getDiscountAmount().compareTo(new BigDecimal("60000")));
        assertEquals(0, quote.getTotalAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("5. Đặt nhiều ghế (1 VIP, 1 COUPLE)")
    void testMultipleSeatsCalculations() {
        Long userId = 1L;
        Long showtimeId = 1L;
        CreateBookingRequestDTO request = new CreateBookingRequestDTO();
        request.setShowtimeId(showtimeId);
        request.setSeatIds(Arrays.asList(10L, 11L));
        request.setPaymentMethod(BookingPaymentMethod.MOMO);

        Showtime showtime = createShowtime(showtimeId, new BigDecimal("100000"));
        Seat seatVIP = createSeat(10L, new BigDecimal("1.20"), "VIP");
        Seat seatCouple = createSeat(11L, new BigDecimal("1.50"), "COUPLE");
        User user = createUser(userId, UserRank.SILVER);

        when(showtimeService.getById(showtimeId)).thenReturn(showtime);
        when(seatService.promoteLocksForCheckout(eq(showtimeId), any(), eq(1L), eq(userId)))
                .thenReturn(Arrays.asList(seatVIP, seatCouple));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(priceConfigService.getPriceMultiplier(any(), any())).thenReturn(new BigDecimal("1.00"));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(foodOrderService.createDraftItems(any(), any())).thenReturn(new ArrayList<>());
        when(foodOrderService.calculateDraftSubtotal(any())).thenReturn(BigDecimal.ZERO);
        when(voucherService.applyVoucherForBooking(any(), any())).thenReturn(BigDecimal.ZERO);

        // Seat subtotal: (1.20 * 100000 * 1.00) + (1.50 * 100000 * 1.00) = 120000 + 150000 = 270000
        // Silver discount: 2% -> factor = 0.98 -> 270000 * 0.98 = 264600
        PrePaymentBookingQuote quote = bookingService.createPrePaymentBooking(userId, request);

        assertNotNull(quote);
        assertEquals(0, quote.getSeatSubtotal().compareTo(new BigDecimal("264600")));
        assertEquals(0, quote.getTotalAmount().compareTo(new BigDecimal("264600")));
    }
}
