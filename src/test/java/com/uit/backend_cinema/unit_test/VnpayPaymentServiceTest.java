package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmation;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.PaymentConfirmationRepository;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.payment.domain.entity.*;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import com.uit.backend_cinema.modules.payment.domain.service.PaymentRequestService;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayIpnResponse;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayPayRequest;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayPayResponse;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayQueryResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.config.VnpayProperties;
import com.uit.backend_cinema.modules.payment.vnpay.domain.repository.VnpayPaymentRepository;
import com.uit.backend_cinema.modules.payment.vnpay.domain.service.VnpayPaymentService;
import com.uit.backend_cinema.modules.payment.vnpay.infrastructure.client.VnpayMerchantClient;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VnpayPaymentServiceTest {

    private final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 17, 10, 0, 0);

    @Mock private VnpayPaymentRepository vnpayPaymentRepository;
    @Mock private PaymentRequestService paymentRequestService;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentReceiptRepository paymentReceiptRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private PaymentConfirmationRepository paymentConfirmationRepository;
    @Mock private TicketService ticketService;
    @Mock private SeatService seatService;
    @Mock private FoodOrderService foodOrderService;
    @Mock private OutboxEventService outboxEventService;
    @Mock private ObjectMapper objectMapper;
    @Mock private Clock clock;
    @Mock private VnpayMerchantClient vnpayMerchantClient;

    private VnpayProperties vnpayProperties;
    private VnpayPaymentService service;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(now.atZone(zone).toInstant());
        when(clock.getZone()).thenReturn(zone);

        vnpayProperties = new VnpayProperties();
        vnpayProperties.setTmnCode("TESTTMN");
        vnpayProperties.setHashSecret("SECRET");
        vnpayProperties.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnpayProperties.setPaymentRequestTtlMinutes(15);

        service = new VnpayPaymentService(
                vnpayProperties, vnpayPaymentRepository, paymentRequestService,
                bookingRepository, paymentReceiptRepository, refundRepository,
                paymentConfirmationRepository, ticketService, seatService,
                foodOrderService, outboxEventService, objectMapper, clock, vnpayMerchantClient);
    }

    private Booking createBooking(Long bookingId, Long userId, BookingStatus status, LocalDateTime deadline) {
        Booking b = new Booking();
        b.setBookingId(bookingId);
        b.setUserId(userId);
        b.setStatus(status);
        b.setPaymentMethod(BookingPaymentMethod.VNPAY);
        b.setTotalAmount(new BigDecimal("100000"));
        b.setShowtimeId(1L);
        b.setPaymentDeadline(deadline);
        b.setExpiresAt(deadline);
        return b;
    }

    @Test
    @DisplayName("createVnpayPayment: Sử dụng paymentDeadline của booking, không gia hạn thêm thời gian")
    void testCreatePaymentUsesBookingDeadline() {
        Long bookingId = 1L;
        Long userId = 10L;
        LocalDateTime deadline = now.plusMinutes(10);
        Booking booking = createBooking(bookingId, userId, BookingStatus.PENDING, deadline);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentRequestService.findLatestByBookingIdAndGateway(bookingId, "VNPAY")).thenReturn(Optional.empty());

        when(vnpayPaymentRepository.createVnpayPayment(any(), any(), any())).thenAnswer(inv -> {
            PaymentGatewayRequest req = inv.getArgument(0);
            req.setPayUrl("https://vnpay.test/pay");
            req.setResultCode(0);
            return req;
        });

        VnpayPayRequest payReq = new VnpayPayRequest();
        payReq.setRequestId("REQ_UUID");

        VnpayPayResponse res = service.createVnpayPayment(userId, false, bookingId, payReq, "127.0.0.1");

        assertNotNull(res);
        ArgumentCaptor<PaymentGatewayRequest> captor = ArgumentCaptor.forClass(PaymentGatewayRequest.class);
        verify(paymentRequestService).save(captor.capture());
        PaymentGatewayRequest saved = captor.getValue();
        assertEquals("20261017101000", saved.getVnpExpireDate());
    }

    @Test
    @DisplayName("createVnpayPayment: Quá hạn paymentDeadline ném BOOKING_EXPIRED")
    void testCreatePaymentAfterDeadlineThrows() {
        Long bookingId = 1L;
        Long userId = 10L;
        LocalDateTime pastDeadline = now.minusMinutes(1);
        Booking booking = createBooking(bookingId, userId, BookingStatus.PENDING, pastDeadline);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        VnpayPayRequest payReq = new VnpayPayRequest();
        payReq.setRequestId("REQ_UUID");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createVnpayPayment(userId, false, bookingId, payReq, "127.0.0.1"));
        assertEquals(ErrorCode.BOOKING_EXPIRED, ex.getCode());
    }

    @Test
    @DisplayName("handleIpnCallback: Giao dịch thành công, đúng hạn, chốt PAID và lưu PaymentReceipt")
    void testIpnCallbackSuccessOnTime() {
        String orderId = "100_1729152000000";
        String txnRef = "VNPAY_TRANS_12345";
        Long bookingId = 100L;
        Long userId = 10L;
        LocalDateTime deadline = now.plusMinutes(5);
        Booking booking = createBooking(bookingId, userId, BookingStatus.PENDING, deadline);

        PaymentIpnResult ipnResult = PaymentIpnResult.builder()
                .success(true)
                .orderId(orderId)
                .transactionId(txnRef)
                .amount(10000000L) // 100,000 * 100
                .payDate("20261017100200")
                .bankCode("NCB")
                .resultCode(0)
                .build();
        when(vnpayPaymentRepository.handleIpn(any())).thenReturn(ipnResult);

        PaymentGatewayRequest pReq = PaymentGatewayRequest.builder()
                .bookingId(bookingId)
                .gateway("VNPAY")
                .orderId(orderId)
                .amount(100000L)
                .status(PaymentGatewayRequestStatus.CREATED)
                .build();
        when(paymentRequestService.findByGatewayAndOrderId("VNPAY", orderId)).thenReturn(Optional.of(pReq));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));
        when(paymentReceiptRepository.findByGatewayAndTransactionReference("VNPAY", txnRef)).thenReturn(Optional.empty());
        when(paymentConfirmationRepository.findByPaymentRef(txnRef)).thenReturn(Optional.empty());

        Map<String, String> params = Map.of("vnp_TxnRef", orderId, "vnp_Amount", "10000000");
        VnpayIpnResponse response = service.handleIpnCallback(params);

        assertEquals("00", response.getRspCode());
        assertEquals(BookingStatus.PAID, booking.getStatus());
        verify(ticketService).finalizeTicketsForPaidBooking(bookingId, 1L);
        verify(paymentReceiptRepository).save(any(PaymentReceipt.class));
        verify(paymentConfirmationRepository).save(any(PaymentConfirmation.class));
    }

    @Test
    @DisplayName("handleIpnCallback: Tiền về sau paymentDeadline, lưu receipt và tạo Refund nghĩa vụ 100%")
    void testIpnCallbackLatePaymentCreatesRefundObligation() {
        String orderId = "100_1729152000000";
        String txnRef = "VNPAY_TRANS_LATE";
        Long bookingId = 100L;
        LocalDateTime expiredDeadline = now.minusMinutes(5);
        Booking booking = createBooking(bookingId, 10L, BookingStatus.EXPIRED, expiredDeadline);

        PaymentIpnResult ipnResult = PaymentIpnResult.builder()
                .success(true)
                .orderId(orderId)
                .transactionId(txnRef)
                .amount(10000000L)
                .payDate("20261017100100") // Trả tiền sau deadline
                .resultCode(0)
                .build();
        when(vnpayPaymentRepository.handleIpn(any())).thenReturn(ipnResult);

        PaymentGatewayRequest pReq = PaymentGatewayRequest.builder()
                .bookingId(bookingId)
                .gateway("VNPAY")
                .orderId(orderId)
                .amount(100000L)
                .status(PaymentGatewayRequestStatus.CREATED)
                .build();
        when(paymentRequestService.findByGatewayAndOrderId("VNPAY", orderId)).thenReturn(Optional.of(pReq));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        PaymentReceipt savedReceipt = PaymentReceipt.builder()
                .paymentReceiptId(999L)
                .bookingId(bookingId)
                .gateway("VNPAY")
                .transactionReference(txnRef)
                .amount(new BigDecimal("100000"))
                .build();
        when(paymentReceiptRepository.save(any())).thenReturn(savedReceipt);

        Map<String, String> params = Map.of("vnp_TxnRef", orderId, "vnp_Amount", "10000000");
        VnpayIpnResponse response = service.handleIpnCallback(params);

        assertEquals("00", response.getRspCode());
        // Booking không bị hồi sinh vé
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
        verify(ticketService, never()).finalizeTicketsForPaidBooking(any(), any());
        // Khoản thu được ghi nhận bền vững
        verify(paymentReceiptRepository).save(any(PaymentReceipt.class));
        // Nghĩa vụ hoàn tiền được tạo
        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());
        Refund refund = refundCaptor.getValue();
        assertEquals(RefundCause.EXPIRED_LATE_PAYMENT, refund.getCause());
        assertEquals(new BigDecimal("100000"), refund.getAmount());
    }

    @Test
    @DisplayName("handleIpnCallback: Giao dịch trùng (booking đã PAID bởi ref khác) tạo Refund DUPLICATE_CHARGE")
    void testIpnCallbackDuplicateChargeCreatesRefund() {
        String orderId = "100_1729152000000";
        String txnRef = "VNPAY_TRANS_DUP";
        Long bookingId = 100L;
        Booking booking = createBooking(bookingId, 10L, BookingStatus.PAID, now.plusMinutes(10));

        PaymentIpnResult ipnResult = PaymentIpnResult.builder()
                .success(true)
                .orderId(orderId)
                .transactionId(txnRef)
                .amount(10000000L)
                .payDate("20261017100100")
                .resultCode(0)
                .build();
        when(vnpayPaymentRepository.handleIpn(any())).thenReturn(ipnResult);

        PaymentGatewayRequest pReq = PaymentGatewayRequest.builder()
                .bookingId(bookingId)
                .gateway("VNPAY")
                .orderId(orderId)
                .amount(100000L)
                .status(PaymentGatewayRequestStatus.CREATED)
                .build();
        when(paymentRequestService.findByGatewayAndOrderId("VNPAY", orderId)).thenReturn(Optional.of(pReq));
        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.of(booking));

        PaymentReceipt savedReceipt = PaymentReceipt.builder()
                .paymentReceiptId(888L)
                .bookingId(bookingId)
                .gateway("VNPAY")
                .transactionReference(txnRef)
                .amount(new BigDecimal("100000"))
                .build();
        when(paymentReceiptRepository.save(any())).thenReturn(savedReceipt);

        Map<String, String> params = Map.of("vnp_TxnRef", orderId, "vnp_Amount", "10000000");
        VnpayIpnResponse response = service.handleIpnCallback(params);

        assertEquals("00", response.getRspCode());
        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());
        assertEquals(RefundCause.DUPLICATE_CHARGE, refundCaptor.getValue().getCause());
    }

    @Test
    @DisplayName("handleIpnCallback: Chữ ký không hợp lệ trả về rspCode 97")
    void testIpnCallbackInvalidSignatureReturns97() {
        when(vnpayPaymentRepository.handleIpn(any())).thenReturn(null);

        Map<String, String> params = Map.of("vnp_TxnRef", "test");
        VnpayIpnResponse response = service.handleIpnCallback(params);

        assertEquals("97", response.getRspCode());
    }
}
