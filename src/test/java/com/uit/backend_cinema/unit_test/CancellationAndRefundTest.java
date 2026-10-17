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
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceipt;
import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundCause;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import com.uit.backend_cinema.modules.payment.domain.service.RefundService;
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
    @Mock private RefundRepository refundRepository;
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

        objectMapper = new ObjectMapper();

        refundService = new RefundService(
                refundRepository, paymentReceiptRepository, null, clock, objectMapper);

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
    @DisplayName("Rạp hủy suất chiếu: Hoàn 100%, hủy vé, giải phóng voucher và tạo Refund SHOWTIME_CANCELLED")
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
        when(paymentReceiptRepository.findByBookingId(bookingId)).thenReturn(List.of(receipt));

        bookingService.refundBookingsForCancelledShowtime(showtimeId, "Sự cố máy chiếu");

        assertEquals(BookingStatus.REFUNDED, booking.getStatus());
        verify(ticketService).cancelTicketsForBooking(bookingId);
        // Rạp hủy giải phóng voucher
        verify(voucherService).releaseVoucherForExpiredBooking(77L);

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());
        Refund savedRefund = refundCaptor.getValue();
        assertEquals(RefundCause.SHOWTIME_CANCELLED, savedRefund.getCause());
        assertEquals(new BigDecimal("150000"), savedRefund.getAmount());
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
