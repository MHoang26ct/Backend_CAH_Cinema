package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.auth.domain.entity.AccountType;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.booking.api.dto.CancelBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.api.dto.CancelBookingResponseDTO;
import com.uit.backend_cinema.modules.booking.api.dto.CancellationQuoteDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.*;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.PaymentConfirmationRepository;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.payment.api.dto.ClaimRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.api.dto.ConfirmCashPayoutResponseDTO;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceipt;
import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundCause;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import com.uit.backend_cinema.modules.payment.domain.service.PaymentRequestService;
import com.uit.backend_cinema.modules.payment.domain.service.RefundService;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.infrastructure.client.VnpayMerchantClient;
import com.uit.backend_cinema.modules.price_config.domain.service.PriceConfigService;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CancellationAndRefundTest {

    private final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 17, 10, 0, 0);

    @Mock private BookingRepository bookingRepository;
    @Mock private SeatService seatService;
    @Mock private ShowtimeService showtimeService;
    @Mock private PriceConfigService priceConfigService;
    @Mock private TicketService ticketService;
    @Mock private FoodOrderService foodOrderService;
    @Mock private VoucherService voucherService;
    @Mock private PaymentConfirmationRepository paymentConfirmationRepository;
    @Mock private PaymentReceiptRepository paymentReceiptRepository;
    @Mock private PaymentRequestService paymentRequestService;
    @Mock private RefundRepository refundRepository;
    @Mock private VnpayMerchantClient vnpayMerchantClient;
    @Mock private OutboxEventService outboxEventService;
    @Mock private UserRepository userRepository;
    @Mock private Clock clock;

    private ObjectMapper objectMapper;
    private BookingService bookingService;
    private RefundService refundService;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(now.atZone(zone).toInstant());
        when(clock.getZone()).thenReturn(zone);

        objectMapper = new ObjectMapper().findAndRegisterModules();
        when(refundRepository.save(any())).thenAnswer(invocation -> {
            Refund refund = invocation.getArgument(0);
            refund.setRefundId(900L);
            return refund;
        });

        refundService = new RefundService(
                refundRepository, paymentReceiptRepository, paymentRequestService, null, clock, objectMapper);

        bookingService = new BookingService(
                bookingRepository, seatService, showtimeService, priceConfigService,
                ticketService, foodOrderService, voucherService, paymentConfirmationRepository,
                paymentReceiptRepository, refundRepository, outboxEventService,
                objectMapper, userRepository, clock, refundService);
    }

    private Booking createPaidBooking(Long bookingId, Long userId, Long showtimeId,
                                      BigDecimal netSeat, BigDecimal netFood, BigDecimal total) {
        Booking b = new Booking();
        b.setBookingId(bookingId);
        b.setUserId(userId);
        b.setShowtimeId(showtimeId);
        b.setStatus(BookingStatus.PAID);
        b.setPaymentMethod(BookingPaymentMethod.VNPAY);
        b.setNetSeatAmount(netSeat);
        b.setNetFoodAmount(netFood);
        b.setTotalAmount(total);
        b.setFoodFulfillmentStatus(FoodFulfillmentStatus.PENDING);
        return b;
    }

    private Showtime createShowtime(Long showtimeId, LocalDateTime startTime) {
        Showtime s = new Showtime();
        s.setShowtimeId(showtimeId);
        s.setStartTime(startTime);
        return s;
    }

    private User createRegisteredUser(Long userId) {
        User u = new User();
        u.setUserId(userId);
        u.setAccountType(AccountType.REGISTERED);
        return u;
    }

    @ParameterizedTest(name = "UT10: {0}h {1}s before showtime")
    @CsvSource({
            "24, -1, true, 0.80, 80000, 50000, 130000, 20000",
            "24,  0, true, 1.00, 100000, 50000, 150000, 0",
            "24,  1, true, 1.00, 100000, 50000, 150000, 0",
            "6,  -1, true, 0.50, 50000, 50000, 100000, 50000",
            "6,   0, true, 0.80, 80000, 50000, 130000, 20000",
            "6,   1, true, 0.80, 80000, 50000, 130000, 20000",
            "2,  -1, false, 0, 0, 0, 0, 150000",
            "2,   0, true, 0.50, 50000, 50000, 100000, 50000",
            "2,   1, true, 0.50, 50000, 50000, 100000, 50000"
    })
    void ut10ExactRefundBoundaries(int hours, int seconds, boolean allowed, String rate,
                                  String ticket, String food, String total, String retained) {
        Booking booking = prepareCancellation();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(10L)).thenReturn(Optional.of(createRegisteredUser(10L)));
        when(showtimeService.getById(100L))
                .thenReturn(createShowtime(100L, now.plusHours(hours).plusSeconds(seconds)));

        CancellationQuoteDTO quote = bookingService.getCancellationQuote(1L, 10L, false);

        assertEquals(allowed, quote.isCanCancel());
        assertMoney(rate, quote.getTicketRefundRate());
        assertMoney(ticket, quote.getTicketRefundAmount());
        assertMoney(food, quote.getFoodRefundAmount());
        assertMoney(total, quote.getTotalRefundAmount());
        assertMoney(retained, quote.getRetainedAmount());
        assertEquals(BookingStatus.PAID, booking.getStatus());
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(refundRepository, paymentReceiptRepository);
    }

    @ParameterizedTest(name = "UT11: net seat {0}, net food {1}")
    @CsvSource({
            "100000.98, 10.02, 100011.00, 50000, 50010.02, 50000.98",
            "100001.00, 10.25, 100011.25, 50001, 50011.25, 50000.00",
            "100001.02, 10.23, 100011.25, 50001, 50011.23, 50000.02"
    })
    void ut11FractionalNetRoundsTicketHalfUpToWholeVndButPreservesFood(
            String netSeat, String netFood, String paid, String ticket, String total, String retained) {
        Booking booking = createPaidBooking(1L, 10L, 100L,
                new BigDecimal(netSeat), new BigDecimal(netFood), new BigDecimal(paid));
        // Gross values are deliberately different: the refund must use the paid net snapshot.
        booking.setSeatSubtotalGross(new BigDecimal("200000"));
        booking.setFoodSubtotalGross(new BigDecimal("1000"));
        when(showtimeService.getById(100L)).thenReturn(createShowtime(100L, now.plusHours(3)));

        CancellationQuoteDTO quote = bookingService.calculateCancellationQuote(booking, now);

        assertTrue(quote.isCanCancel());
        assertMoney("0.50", quote.getTicketRefundRate());
        assertMoney(ticket, quote.getTicketRefundAmount());
        assertMoney(netFood, quote.getFoodRefundAmount());
        assertMoney(total, quote.getTotalRefundAmount());
        assertMoney(retained, quote.getRetainedAmount());
        assertMoney(netSeat, booking.getNetSeatAmount());
        assertMoney(netFood, booking.getNetFoodAmount());
        verifyNoInteractions(refundRepository, paymentReceiptRepository);
    }

    @ParameterizedTest(name = "UT18: {0}, checked-in={1}, seconds until show={2}")
    @CsvSource({
            "PENDING, false, 90000, BOOKING_CANNOT_BE_CANCELLED",
            "CHECKED_IN, true, 90000, BOOKING_CANNOT_BE_CANCELLED",
            "PAID, true, 90000, BOOKING_ALREADY_CHECKED_IN",
            "PAID, false, 7199, BOOKING_CANCEL_DEADLINE_EXCEEDED"
    })
    void ut18CancellationGuardsDoNotWriteOrReleaseResources(BookingStatus status, boolean checkedIn,
                                                            long seconds, ErrorCode expectedCode) {
        Booking booking = prepareCancellation();
        booking.setStatus(status);
        when(userRepository.findById(10L)).thenReturn(Optional.of(createRegisteredUser(10L)));
        when(ticketService.hasAnyCheckedInTicket(1L)).thenReturn(checkedIn);
        when(showtimeService.getById(100L)).thenReturn(createShowtime(100L, now.plusSeconds(seconds)));

        BusinessException error = assertThrows(BusinessException.class, () -> bookingService.cancelBooking(
                1L, 10L, false, CancelBookingRequestDTO.builder().expectedRefundAmount(new BigDecimal("150000")).build()));

        assertEquals(expectedCode, error.getCode());
        assertEquals(status, booking.getStatus());
        assertNull(booking.getCancellationPolicySnapshot());
        verify(bookingRepository, never()).save(any());
        verify(ticketService, never()).cancelTicketsForBooking(any());
        verifyNoInteractions(refundRepository, paymentReceiptRepository, seatService, voucherService, foodOrderService);
    }

    @ParameterizedTest(name = "UT18: cancellation at 2h + {0}s is allowed")
    @CsvSource({"0", "1"})
    void ut18CancellationAtOrAboveTwoHoursCreatesObligationNotPayout(int seconds) {
        Booking booking = prepareCancellation();
        prepareReceipt(booking);
        when(userRepository.findById(10L)).thenReturn(Optional.of(createRegisteredUser(10L)));
        when(showtimeService.getById(100L))
                .thenReturn(createShowtime(100L, now.plusHours(2).plusSeconds(seconds)));

        CancelBookingResponseDTO result = bookingService.cancelBooking(1L, 10L, false,
                CancelBookingRequestDTO.builder().expectedRefundAmount(new BigDecimal("100000")).build());

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        assertEquals(RefundStatus.REQUESTED, result.getRefundStatus());
        assertMoney("100000", result.getRefundAmount());
        assertMoney("50000", result.getRetainedAmount());
        ArgumentCaptor<Refund> capture = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(capture.capture());
        assertMoney("100000", capture.getValue().getAmount());
        assertEquals(RefundStatus.REQUESTED, capture.getValue().getStatus());
        assertEquals(RefundCause.CUSTOMER_CANCELLED, capture.getValue().getCause());
        assertEquals(555L, capture.getValue().getPaymentReceiptId());
        verify(bookingRepository).save(booking);
        verify(ticketService).cancelTicketsForBooking(1L);
        verifyNoInteractions(vnpayMerchantClient, voucherService);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "Expected amount " + expected + ", got " + actual);
    }

    @Test
    @DisplayName("Báo giá hủy: >= 24 giờ trước chiếu -> hoàn vé 100% + 100% đồ ăn chưa giao")
    void testCancellationQuoteAt24h() {
        Long bookingId = 1L;
        Long userId = 10L;
        Long showtimeId = 100L;
        // Showtime bắt đầu sau 25 giờ
        LocalDateTime startTime = now.plusHours(25);

        Booking booking = createPaidBooking(bookingId, userId, showtimeId,
                new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000"));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(showtimeService.getById(showtimeId)).thenReturn(createShowtime(showtimeId, startTime));
        when(ticketService.hasAnyCheckedInTicket(bookingId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(createRegisteredUser(userId)));

        CancellationQuoteDTO quote = bookingService.getCancellationQuote(bookingId, userId, false);

        assertTrue(quote.isCanCancel());
        assertEquals(new BigDecimal("1.00"), quote.getTicketRefundRate());
        assertEquals(new BigDecimal("100000"), quote.getTicketRefundAmount());
        assertEquals(new BigDecimal("50000"), quote.getFoodRefundAmount());
        assertEquals(new BigDecimal("150000"), quote.getTotalRefundAmount());
        assertEquals(BigDecimal.ZERO, quote.getRetainedAmount());
    }

    @Test
    @DisplayName("Báo giá hủy: >= 6 giờ trước chiếu -> hoàn vé 80% + 100% đồ ăn chưa giao")
    void testCancellationQuoteAt6h() {
        Long bookingId = 1L;
        Long userId = 10L;
        Long showtimeId = 100L;
        // Showtime bắt đầu sau 10 giờ
        LocalDateTime startTime = now.plusHours(10);

        Booking booking = createPaidBooking(bookingId, userId, showtimeId,
                new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000"));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(showtimeService.getById(showtimeId)).thenReturn(createShowtime(showtimeId, startTime));
        when(ticketService.hasAnyCheckedInTicket(bookingId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(createRegisteredUser(userId)));

        CancellationQuoteDTO quote = bookingService.getCancellationQuote(bookingId, userId, false);

        assertTrue(quote.isCanCancel());
        assertEquals(new BigDecimal("0.80"), quote.getTicketRefundRate());
        assertEquals(new BigDecimal("80000"), quote.getTicketRefundAmount());
        assertEquals(new BigDecimal("50000"), quote.getFoodRefundAmount());
        assertEquals(new BigDecimal("130000"), quote.getTotalRefundAmount());
        assertEquals(new BigDecimal("20000"), quote.getRetainedAmount());
    }

    @Test
    @DisplayName("Báo giá hủy: >= 2 giờ trước chiếu -> hoàn vé 50% + 100% đồ ăn chưa giao")
    void testCancellationQuoteAt2h() {
        Long bookingId = 1L;
        Long userId = 10L;
        Long showtimeId = 100L;
        // Showtime bắt đầu sau 3 giờ
        LocalDateTime startTime = now.plusHours(3);

        Booking booking = createPaidBooking(bookingId, userId, showtimeId,
                new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000"));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(showtimeService.getById(showtimeId)).thenReturn(createShowtime(showtimeId, startTime));
        when(ticketService.hasAnyCheckedInTicket(bookingId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(createRegisteredUser(userId)));

        CancellationQuoteDTO quote = bookingService.getCancellationQuote(bookingId, userId, false);

        assertTrue(quote.isCanCancel());
        assertEquals(new BigDecimal("0.50"), quote.getTicketRefundRate());
        assertEquals(new BigDecimal("50000"), quote.getTicketRefundAmount());
        assertEquals(new BigDecimal("50000"), quote.getFoodRefundAmount());
        assertEquals(new BigDecimal("100000"), quote.getTotalRefundAmount());
        assertEquals(new BigDecimal("50000"), quote.getRetainedAmount());
    }

    @Test
    @DisplayName("Báo giá hủy: < 2 giờ trước chiếu -> canCancel = false")
    void testCancellationQuoteUnder2hForbidden() {
        Long bookingId = 1L;
        Long userId = 10L;
        Long showtimeId = 100L;
        // Showtime bắt đầu sau 1 giờ 30 phút
        LocalDateTime startTime = now.plusMinutes(90);

        Booking booking = createPaidBooking(bookingId, userId, showtimeId,
                new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000"));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(showtimeService.getById(showtimeId)).thenReturn(createShowtime(showtimeId, startTime));
        when(ticketService.hasAnyCheckedInTicket(bookingId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(createRegisteredUser(userId)));

        CancellationQuoteDTO quote = bookingService.getCancellationQuote(bookingId, userId, false);

        assertFalse(quote.isCanCancel());
        assertNotNull(quote.getReason());
        assertEquals(BigDecimal.ZERO, quote.getTotalRefundAmount());
    }

    @Test
    @DisplayName("Báo giá hủy: Đồ ăn đã FULFILLED thì foodRefundAmount = 0")
    void testCancellationQuoteFoodFulfilledNoRefund() {
        Long bookingId = 1L;
        Long userId = 10L;
        Long showtimeId = 100L;
        LocalDateTime startTime = now.plusHours(25);

        Booking booking = createPaidBooking(bookingId, userId, showtimeId,
                new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000"));
        booking.setFoodFulfillmentStatus(FoodFulfillmentStatus.FULFILLED);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(showtimeService.getById(showtimeId)).thenReturn(createShowtime(showtimeId, startTime));
        when(ticketService.hasAnyCheckedInTicket(bookingId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(createRegisteredUser(userId)));

        CancellationQuoteDTO quote = bookingService.getCancellationQuote(bookingId, userId, false);

        assertTrue(quote.isCanCancel());
        assertEquals(new BigDecimal("100000"), quote.getTicketRefundAmount());
        assertEquals(BigDecimal.ZERO, quote.getFoodRefundAmount());
        assertEquals(new BigDecimal("100000"), quote.getTotalRefundAmount());
    }

    @Test
    @DisplayName("Hủy booking hợp lệ: Chuyển CANCELLED, hủy vé, tạo Refund obligation, không trả voucher")
    void testCancelBookingSuccess() {
        Long bookingId = 1L;
        Long userId = 10L;
        Long showtimeId = 100L;
        LocalDateTime startTime = now.plusHours(25);

        Booking booking = createPaidBooking(bookingId, userId, showtimeId,
                new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000"));
        booking.setVoucherId(99L);
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(showtimeService.getById(showtimeId)).thenReturn(createShowtime(showtimeId, startTime));
        when(ticketService.hasAnyCheckedInTicket(bookingId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(createRegisteredUser(userId)));

        PaymentReceipt receipt = PaymentReceipt.builder()
                .paymentReceiptId(555L)
                .bookingId(bookingId)
                .gateway("VNPAY")
                .amount(new BigDecimal("150000"))
                .build();
        receipt.setTransactionReference("settled");
        receipt.setStatus(com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceiptStatus.RECORDED);
        receipt.setVerifiedAt(now);
        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setGateway("VNPAY");
        confirmation.setPaymentRef("settled");
        when(paymentConfirmationRepository.findByBookingIdAndStatus(bookingId, PaymentConfirmationStatus.SUCCESS))
                .thenReturn(Optional.of(confirmation));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(paymentReceiptRepository.findByBookingId(bookingId)).thenReturn(List.of(receipt));

        CancelBookingRequestDTO req = CancelBookingRequestDTO.builder()
                .expectedRefundAmount(new BigDecimal("150000"))
                .build();

        CancelBookingResponseDTO res = bookingService.cancelBooking(bookingId, userId, false, req);

        assertNotNull(res);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(ticketService).cancelTicketsForBooking(bookingId);
        // Khách tự hủy không hoàn trả voucher
        verify(voucherService, never()).releaseVoucherForExpiredBooking(any());

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());
        Refund savedRefund = refundCaptor.getValue();
        assertEquals(RefundCause.CUSTOMER_CANCELLED, savedRefund.getCause());
        assertEquals(new BigDecimal("150000"), savedRefund.getAmount());
        assertEquals(RefundStatus.REQUESTED, savedRefund.getStatus());
    }

    @Test
    @DisplayName("Hủy booking: Tiền hoàn kỳ vọng không khớp báo giá (do mốc thời gian nhảy bậc) ném QUOTE_EXPIRED_OR_CHANGED")
    void testCancelBookingExpectedAmountMismatch() {
        Long bookingId = 1L;
        Long userId = 10L;
        Long showtimeId = 100L;
        // Showtime sau 10 tiếng (tỷ lệ 80% -> hoàn 130,000)
        LocalDateTime startTime = now.plusHours(10);

        Booking booking = createPaidBooking(bookingId, userId, showtimeId,
                new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000"));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(showtimeService.getById(showtimeId)).thenReturn(createShowtime(showtimeId, startTime));
        when(ticketService.hasAnyCheckedInTicket(bookingId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(createRegisteredUser(userId)));

        // Client gửi expectedRefundAmount = 150000 (do xem màn hình từ lúc trước 24h)
        CancelBookingRequestDTO req = CancelBookingRequestDTO.builder()
                .expectedRefundAmount(new BigDecimal("150000"))
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.cancelBooking(bookingId, userId, false, req));
        assertEquals(ErrorCode.QUOTE_EXPIRED_OR_CHANGED, ex.getCode());
    }

    @Test
    @DisplayName("Rạp hủy suất chiếu: Hoàn 100%, hủy vé, giữ voucher đã dùng và tạo Refund SHOWTIME_CANCELLED")
    void testRefundBookingsForCancelledShowtime() {
        Long showtimeId = 100L;
        Long bookingId = 1L;
        Booking booking = createPaidBooking(bookingId, 10L, showtimeId,
                new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000"));
        booking.setVoucherId(77L);
        when(bookingRepository.findActiveByShowtimeId(showtimeId)).thenReturn(List.of(booking));

        PaymentReceipt receipt = PaymentReceipt.builder()
                .paymentReceiptId(555L)
                .bookingId(bookingId)
                .gateway("VNPAY")
                .amount(new BigDecimal("150000"))
                .build();
        receipt.setTransactionReference("settled");
        receipt.setStatus(com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceiptStatus.RECORDED);
        receipt.setVerifiedAt(now);
        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setGateway("VNPAY");
        confirmation.setPaymentRef("settled");
        when(paymentConfirmationRepository.findByBookingIdAndStatus(bookingId, PaymentConfirmationStatus.SUCCESS))
                .thenReturn(Optional.of(confirmation));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(paymentReceiptRepository.findByBookingId(bookingId)).thenReturn(List.of(receipt));

        bookingService.refundBookingsForCancelledShowtime(showtimeId, "Sự cố máy chiếu");

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(ticketService).cancelTicketsForBooking(bookingId);
        // Voucher của booking đã thanh toán không được trả lại, kể cả khi rạp hủy suất.
        verify(voucherService, never()).releaseVoucherForExpiredBooking(77L);

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());
        Refund savedRefund = refundCaptor.getValue();
        assertEquals(RefundCause.SHOWTIME_CANCELLED, savedRefund.getCause());
        assertEquals(new BigDecimal("150000"), savedRefund.getAmount());
    }

    private Booking prepareCancellation() {
        Booking booking = createPaidBooking(1L, 10L, 100L,
                new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000"));
        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(showtimeService.getById(100L)).thenReturn(createShowtime(100L, now.plusHours(10)));
        return booking;
    }

    private PaymentReceipt prepareReceipt(Booking booking) {
        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setPaymentRef("funding-payment");
        confirmation.setGateway("STRIPE");
        when(paymentConfirmationRepository.findByBookingIdAndStatus(1L, PaymentConfirmationStatus.SUCCESS))
                .thenReturn(Optional.of(confirmation));
        PaymentReceipt receipt = PaymentReceipt.builder().paymentReceiptId(555L).bookingId(1L)
                .gateway("STRIPE").transactionReference("funding-payment").verifiedAt(now)
                .amount(booking.getTotalAmount())
                .status(com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceiptStatus.RECORDED).build();
        when(paymentReceiptRepository.findByBookingId(1L)).thenReturn(List.of(receipt));
        return receipt;
    }

    @Test
    void missingQuoteConfirmationCannotCancel() {
        Booking booking = prepareCancellation();
        for (CancelBookingRequestDTO request : java.util.Arrays.asList(null, new CancelBookingRequestDTO(),
                CancelBookingRequestDTO.builder().quoteToken(" ").build())) {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.cancelBooking(1L, 10L, true, request));
            assertEquals(ErrorCode.QUOTE_EXPIRED_OR_CHANGED, ex.getCode());
        }
        assertEquals(BookingStatus.PAID, booking.getStatus());
        verifyNoInteractions(refundRepository);
        verify(ticketService, never()).cancelTicketsForBooking(any());
    }

    @Test
    void staleTokenCannotBeBypassedByCorrectAmount() {
        prepareCancellation();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> bookingService.cancelBooking(1L, 10L, true, CancelBookingRequestDTO.builder()
                        .quoteToken("QUOTE_1_150000_1.00").expectedRefundAmount(new BigDecimal("130000")).build()));
        assertEquals(ErrorCode.QUOTE_EXPIRED_OR_CHANGED, ex.getCode());
        verifyNoInteractions(refundRepository);
    }

    @Test
    void tokenOnlyConfirmationUsesFundingReceiptNotFirstCollection() {
        Booking booking = prepareCancellation();
        PaymentReceipt funding = prepareReceipt(booking);
        PaymentReceipt duplicate = PaymentReceipt.builder().paymentReceiptId(444L).bookingId(1L)
                .gateway("STRIPE").transactionReference("duplicate-payment").amount(booking.getTotalAmount())
                .verifiedAt(now).status(com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceiptStatus.RECORDED).build();
        when(paymentReceiptRepository.findByBookingId(1L)).thenReturn(List.of(duplicate, funding));
        String token = bookingService.calculateCancellationQuote(booking, now).getQuoteToken();
        CancelBookingResponseDTO result = bookingService.cancelBooking(1L, 10L, true,
                CancelBookingRequestDTO.builder().quoteToken(token).build());
        assertEquals(RefundStatus.REQUESTED, result.getRefundStatus());
        assertEquals(900L, result.getRefundId());
        verify(refundRepository).save(argThat(r -> r.getPaymentReceiptId().equals(555L)));
    }

    @Test
    void missingReceiptFailsBeforeCancellationForCustomerAndAdmin() {
        Booking booking = prepareCancellation();
        when(bookingRepository.findActiveByShowtimeId(100L)).thenReturn(List.of(booking));
        assertThrows(BusinessException.class, () -> bookingService.cancelBooking(1L, 10L, true,
                CancelBookingRequestDTO.builder().expectedRefundAmount(new BigDecimal("130000")).build()));
        assertThrows(BusinessException.class, () -> bookingService.refundBookingsForCancelledShowtime(100L, "maintenance"));
        assertEquals(BookingStatus.PAID, booking.getStatus());
        verify(bookingRepository, never()).save(any());
        verify(ticketService, never()).cancelTicketsForBooking(any());
        verifyNoInteractions(refundRepository);
    }

    @Test
    void refundInsertFailureDoesNotReportCancellationSuccess() {
        Booking booking = prepareCancellation();
        prepareReceipt(booking);
        doThrow(new org.springframework.dao.DataIntegrityViolationException("storage failed")).when(refundRepository).save(any());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> bookingService.cancelBooking(1L, 10L, true,
                        CancelBookingRequestDTO.builder().expectedRefundAmount(new BigDecimal("130000")).build()));
        assertEquals(BookingStatus.PAID, booking.getStatus());
        verify(bookingRepository, never()).save(any());
        verify(ticketService, never()).cancelTicketsForBooking(any());
    }

    @Test
    void quarantinedHistoricalSnapshotBlocksQuoteAndCancelWithoutSideEffects() {
        Booking booking = prepareCancellation();
        booking.setLegacySnapshotStatus("REVIEW_REQUIRED");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        assertThrows(BusinessException.class, () -> bookingService.getCancellationQuote(1L, 10L, true));
        assertThrows(BusinessException.class, () -> bookingService.cancelBooking(1L, 10L, true,
                CancelBookingRequestDTO.builder().expectedRefundAmount(new BigDecimal("130000")).build()));
        verifyNoInteractions(refundRepository);
        verify(bookingRepository, never()).save(any());
        verify(ticketService, never()).cancelTicketsForBooking(any());
    }

    @Test
    void staffPendingBookingIsCancelledBeforeDeadlineAndResourcesReleasedOnlyOnce() {
        Booking booking = prepareCancellation();
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(now.plusMinutes(15));
        booking.setCreatedByStaffId(20L);
        booking.setVoucherId(99L);
        when(bookingRepository.findActiveByShowtimeId(100L)).thenReturn(List.of(booking));
        when(ticketService.findActiveDraftSeatIds(1L)).thenReturn(List.of(7L));
        bookingService.refundBookingsForCancelledShowtime(100L, "maintenance");
        bookingService.refundBookingsForCancelledShowtime(100L, "retry");
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(bookingRepository, times(1)).save(booking);
        verify(seatService, times(1)).releaseSeatLocksByOwner(100L, List.of(7L), 20L);
        verify(voucherService, times(1)).releaseVoucherForExpiredBooking(99L);
        verify(ticketService, times(1)).expireDraftItems(1L);
        verify(foodOrderService, times(1)).expireDraftItems(1L);
        verify(bookingRepository, never()).markExpiredIfPendingAndExpired(any(), any());
        verifyNoInteractions(refundRepository);
    }

    @Test
    void quoteTokenBecomesInvalidAfterFoodIsFulfilled() {
        Booking booking = prepareCancellation();
        String token = bookingService.calculateCancellationQuote(booking, now).getQuoteToken();
        booking.setFoodFulfillmentStatus(FoodFulfillmentStatus.FULFILLED);
        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.cancelBooking(1L, 10L, true,
                CancelBookingRequestDTO.builder().quoteToken(token).build()));
        assertEquals(ErrorCode.QUOTE_EXPIRED_OR_CHANGED, ex.getCode());
    }

    @Test
    void adminCancellationAlsoSelectsFundingReceipt() {
        Booking booking = prepareCancellation();
        PaymentReceipt funding = prepareReceipt(booking);
        when(paymentReceiptRepository.findByBookingId(1L)).thenReturn(List.of(
                PaymentReceipt.builder().paymentReceiptId(444L).bookingId(1L).gateway("VNPAY")
                        .transactionReference("funding-payment").build(), funding));
        when(bookingRepository.findActiveByShowtimeId(100L)).thenReturn(List.of(booking));
        bookingService.refundBookingsForCancelledShowtime(100L, "maintenance");
        verify(refundRepository).save(argThat(r -> r.getPaymentReceiptId().equals(555L)
                && r.getAmount().compareTo(new BigDecimal("150000")) == 0));
    }

    @Test
    @DisplayName("Hoàn tiền mặt CASH: Staff claim và confirm chi tiền thành công")
    void testCashRefundClaimAndConfirmPayout() {
        Long refundId = 10L;
        Long staffA = 1001L;

        PaymentReceipt receipt = PaymentReceipt.builder()
                .paymentReceiptId(20L)
                .gateway("CASH")
                .amount(new BigDecimal("100000"))
                .build();
        when(paymentReceiptRepository.findById(20L)).thenReturn(Optional.of(receipt));

        Refund refund = Refund.builder()
                .refundId(refundId)
                .paymentReceiptId(20L)
                .bookingId(5L)
                .amount(new BigDecimal("100000"))
                .status(RefundStatus.REQUESTED)
                .build();
        when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));
        when(refundRepository.claimCashRefund(eq(refundId), eq(staffA), any())).thenReturn(true);

        ClaimRefundResponseDTO claimRes = refundService.claimCashRefund(refundId, staffA);
        assertNotNull(claimRes);
        assertEquals(RefundStatus.PROCESSING, claimRes.getStatus());
        assertEquals(staffA, claimRes.getActorStaffId());

        // Update mock trạng thái đã claim bởi staffA
        refund.setStatus(RefundStatus.PROCESSING);
        refund.setActorStaffId(staffA);

        when(refundRepository.confirmCashPayout(eq(refundId), eq(staffA), any())).thenReturn(true);
        ConfirmCashPayoutResponseDTO confirmRes = refundService.confirmCashPayout(refundId, staffA);
        assertNotNull(confirmRes);
        assertEquals(RefundStatus.SUCCEEDED, confirmRes.getStatus());
    }

    @Test
    @DisplayName("Hoàn tiền mặt CASH: Staff B không được xác nhận chi tiền khi Staff A đã claim")
    void testCashRefundOtherStaffCannotConfirm() {
        Long refundId = 10L;
        Long staffA = 1001L;
        Long staffB = 1002L;

        PaymentReceipt receipt = PaymentReceipt.builder()
                .paymentReceiptId(20L)
                .gateway("CASH")
                .amount(new BigDecimal("100000"))
                .build();
        when(paymentReceiptRepository.findById(20L)).thenReturn(Optional.of(receipt));

        Refund refund = Refund.builder()
                .refundId(refundId)
                .paymentReceiptId(20L)
                .bookingId(5L)
                .amount(new BigDecimal("100000"))
                .status(RefundStatus.PROCESSING)
                .actorStaffId(staffA)
                .build();
        when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.confirmCashPayout(refundId, staffB));
        assertEquals(ErrorCode.REFUND_STAFF_MISMATCH, ex.getCode());
    }

    @Test
    @DisplayName("Refund VNPay dùng vnp_CreateDate gốc của payment request, không dùng PayDate")
    void testOnlineRefundUsesOriginalVnpCreateDate() {
        PaymentReceipt receipt = PaymentReceipt.builder()
                .paymentReceiptId(20L)
                .paymentRequestId(30L)
                .bookingId(5L)
                .gateway("VNPAY")
                .transactionReference("VNPAY_TRANS_001")
                .orderId("ORDER_001")
                .amount(new BigDecimal("100000"))
                .payDate(now.minusMinutes(5))
                .build();
        Refund refund = Refund.builder()
                .refundId(10L)
                .paymentReceiptId(20L)
                .bookingId(5L)
                .amount(new BigDecimal("100000"))
                .status(RefundStatus.REQUESTED)
                .build();
        PaymentGatewayRequest paymentRequest = PaymentGatewayRequest.builder()
                .paymentRequestId(30L)
                .vnpCreateDate("20261017100000")
                .build();

        when(refundRepository.findById(10L)).thenReturn(Optional.of(refund));
        when(paymentReceiptRepository.findById(20L)).thenReturn(Optional.of(receipt));
        when(paymentRequestService.findById(30L)).thenReturn(Optional.of(paymentRequest));
        when(vnpayMerchantClient.refundTransaction(
                anyString(), any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(VnpayRefundResponseDTO.builder().responseCode("00").transactionStatus("00").build());

        RefundService onlineRefundService = new RefundService(
                refundRepository, paymentReceiptRepository, paymentRequestService, vnpayMerchantClient, clock, objectMapper,
                mock(org.springframework.transaction.PlatformTransactionManager.class));
        when(refundRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(refund));
        onlineRefundService.processOnlineRefund(10L);

        verify(vnpayMerchantClient).refundTransaction(
                eq("ORDER_001"), eq(new BigDecimal("100000")), eq("02"), eq("VNPAY_TRANS_001"),
                eq("20261017100000"), eq("system"), anyString(), eq("127.0.0.1"));
        assertEquals(RefundStatus.SUCCEEDED, refund.getStatus());
    }

    @Test
    @DisplayName("Refund VNPay thiếu vnp_CreateDate phải chuyển review, không gửi request tham chiếu sai")
    void testOnlineRefundWithoutOriginalCreateDateRequiresReview() {
        PaymentReceipt receipt = PaymentReceipt.builder()
                .paymentReceiptId(20L)
                .paymentRequestId(30L)
                .bookingId(5L)
                .gateway("VNPAY")
                .amount(new BigDecimal("100000"))
                .build();
        Refund refund = Refund.builder()
                .refundId(10L)
                .paymentReceiptId(20L)
                .bookingId(5L)
                .amount(new BigDecimal("100000"))
                .status(RefundStatus.REQUESTED)
                .build();

        when(refundRepository.findById(10L)).thenReturn(Optional.of(refund));
        when(paymentReceiptRepository.findById(20L)).thenReturn(Optional.of(receipt));
        when(paymentRequestService.findById(30L)).thenReturn(Optional.of(PaymentGatewayRequest.builder().build()));

        RefundService onlineRefundService = new RefundService(
                refundRepository, paymentReceiptRepository, paymentRequestService, vnpayMerchantClient, clock, objectMapper,
                mock(org.springframework.transaction.PlatformTransactionManager.class));
        when(refundRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(refund));
        onlineRefundService.processOnlineRefund(10L);

        assertEquals(RefundStatus.REVIEW_REQUIRED, refund.getStatus());
        verifyNoInteractions(vnpayMerchantClient);
    }

    @Test
    @DisplayName("Hoàn tiền mặt CASH: Không thể claim chi trả tiền mặt cho giao dịch VNPay")
    void testCannotCashPayoutVnpayRefund() {
        Long refundId = 10L;
        Long staffA = 1001L;

        PaymentReceipt receipt = PaymentReceipt.builder()
                .paymentReceiptId(20L)
                .gateway("VNPAY")
                .amount(new BigDecimal("100000"))
                .build();
        when(paymentReceiptRepository.findById(20L)).thenReturn(Optional.of(receipt));

        Refund refund = Refund.builder()
                .refundId(refundId)
                .paymentReceiptId(20L)
                .bookingId(5L)
                .status(RefundStatus.REQUESTED)
                .build();
        when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> refundService.claimCashRefund(refundId, staffA));
        assertEquals(ErrorCode.REFUND_METHOD_MISMATCH, ex.getCode());
    }
}
