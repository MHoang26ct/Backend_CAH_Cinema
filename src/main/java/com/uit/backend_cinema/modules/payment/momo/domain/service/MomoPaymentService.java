package com.uit.backend_cinema.modules.payment.momo.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequestStatus;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentIpnResult;
import com.uit.backend_cinema.modules.payment.domain.service.PaymentRequestService;
import com.uit.backend_cinema.modules.payment.momo.api.dto.MomoIpnRequest;
import com.uit.backend_cinema.modules.payment.momo.api.dto.MomoPayRequest;
import com.uit.backend_cinema.modules.payment.momo.api.dto.MomoPayResponse;
import com.uit.backend_cinema.modules.payment.momo.domain.config.MomoPaymentConfig;
import com.uit.backend_cinema.modules.payment.momo.domain.repository.MomoPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class MomoPaymentService {

    private static final Logger log = LoggerFactory.getLogger(MomoPaymentService.class);
    private static final String GATEWAY = "MOMO";

    private final MomoPaymentConfig momoConfig;
    private final MomoPaymentRepository momoPaymentRepository;
    private final PaymentRequestService paymentRequestService;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public MomoPaymentService(MomoPaymentConfig momoConfig,
                              MomoPaymentRepository momoPaymentRepository,
                              PaymentRequestService paymentRequestService,
                              BookingRepository bookingRepository,
                              BookingService bookingService) {
        this.momoConfig = momoConfig;
        this.momoPaymentRepository = momoPaymentRepository;
        this.paymentRequestService = paymentRequestService;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    /**
     * Tạo đơn thanh toán MoMo cho booking.
     * Nếu booking đã có payment request CREATED → trả lại payUrl cũ (không gọi MoMo lại).
     */
    @Transactional
    public MomoPayResponse createMomoPayment(Long userId, String email, boolean isStaffOrAdmin, Long bookingId, MomoPayRequest payRequest) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));

        // Validate
        if (!booking.getUserId().equals(userId) && !isStaffOrAdmin) {
            throw new BusinessException("Bạn không có quyền thanh toán booking này", ErrorCode.FORBIDDEN);
        }
        if (booking.getPaymentMethod() != BookingPaymentMethod.MOMO) {
            throw new BusinessException("Booking này không dùng phương thức thanh toán MoMo", ErrorCode.BOOKING_INVALID_STATUS);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Booking không ở trạng thái chờ thanh toán", ErrorCode.BOOKING_INVALID_STATUS);
        }
        if (!booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Booking đã hết hạn thanh toán", ErrorCode.BOOKING_EXPIRED);
        }

        // 1 query duy nhất — lấy payment request mới nhất của booking
        Optional<PaymentGatewayRequest> existingOpt =
                paymentRequestService.findLatestByBookingId(bookingId);

        if (existingOpt.isPresent()) {
            PaymentGatewayRequest ex = existingOpt.get();

            if (ex.getStatus() == PaymentGatewayRequestStatus.CREATED) {
                boolean isExpired = ex.getCreatedAt()
                        .isBefore(LocalDateTime.now().minusMinutes(momoConfig.getPaymentRequestTtlMinutes()));

                if (!isExpired) {
                    // QR vẫn còn sống: luôn trả lại QR cũ,
                    // dù client gửi cùng hay khác requestId (QR vẫn quét được)
                    log.info("[MoMo] Booking {} có payment request CREATED còn hiệu lực (requestId={}), trả lại QR cũ",
                            bookingId, ex.getRequestId());
                    return MomoPayResponse.builder()
                            .payUrl(ex.getPayUrl())
                            .deeplink(ex.getDeeplink())
                            .qrCodeUrl(ex.getQrCodeUrl())
                            .momoOrderId(ex.getOrderId())
                            .build();
                }

                // QR hết TTL → đánh FAILED rồi tạo đơn mới
                log.info("[MoMo] Booking {} có payment request CREATED nhưng đã hết TTL, tạo đơn mới", bookingId);
                ex.setStatus(PaymentGatewayRequestStatus.FAILED);
                ex.setResponseMessage("QR hết hạn sau " + momoConfig.getPaymentRequestTtlMinutes() + " phút không thanh toán");
                paymentRequestService.save(ex);
            }
            // status == FAILED → fall through, tạo đơn mới
        }

        // Tạo đơn mới
        // orderId = bookingId_timestamp — unique mỗi lần tạo, tránh trùng ở MoMo
        String orderId = bookingId.toString() + "_" + System.currentTimeMillis();
        String clientRequestId = payRequest.getRequestId(); // lưu vào DB để trace
        // server sinh UUID riêng gửi lên MoMo — đảm bảo luôn unique, tránh MoMo reject trùng requestId
        String gatewayRequestId = UUID.randomUUID().toString();
        long amount = booking.getTotalAmount().longValue();
        String orderInfo = "Thanh toan booking #" + bookingId;

        // Build domain request object
        PaymentGatewayRequest request = PaymentGatewayRequest.builder()
                .bookingId(bookingId)
                .gateway(GATEWAY)
                .requestId(clientRequestId)
                .gatewayRequestId(gatewayRequestId)
                .orderId(orderId)
                .amount(amount)
                .orderInfo(orderInfo)
                .requestType(payRequest.getRequestType())
                .customerEmail(email)
                .build();

        // Gọi gateway tạo đơn thanh toán qua domain interface
        PaymentGatewayRequest result = momoPaymentRepository.createPayment(request);

        // Lưu payment request (dù thành công hay thất bại đều lưu để trace)
        paymentRequestService.save(result);

        if (result.getResultCode() != 0) {
            log.error("[MoMo] Tạo đơn thất bại cho booking {}, resultCode={}, message={}", bookingId, result.getResultCode(), result.getResponseMessage());
            throw new BusinessException("Tạo đơn MoMo thất bại: " + result.getResponseMessage(), ErrorCode.MOMO_PAYMENT_CREATION_FAILED);
        }

        log.info("[MoMo] Tạo đơn thành công cho booking {}, orderId={}", bookingId, orderId);
        return MomoPayResponse.builder()
                .payUrl(result.getPayUrl())
                .deeplink(result.getDeeplink())
                .qrCodeUrl(result.getQrCodeUrl())
                .momoOrderId(orderId)
                .build();
    }

    /**
     * Xử lý IPN callback từ MoMo (server-to-server).
     * Luôn return void — controller trả HTTP 204 cho MoMo.
     */
    @Transactional
    public void handleIpnCallback(MomoIpnRequest ipn) {
        log.info("[MoMo IPN] orderId={}, resultCode={}, transId={}", ipn.getOrderId(), ipn.getResultCode(), ipn.getTransId());

        // 1. Verify & parse IPN qua domain interface
        PaymentIpnResult ipnResult = momoPaymentRepository.handleIpn(ipn);
        if (ipnResult == null) {
            log.error("[MoMo IPN] Signature không hợp lệ cho orderId={}", ipn.getOrderId());
            throw new BusinessException("Chữ ký MoMo IPN không hợp lệ", ErrorCode.MOMO_SIGNATURE_INVALID);
        }

        // 2. Tìm payment request theo orderId
        PaymentGatewayRequest paymentReq = paymentRequestService
                .findByGatewayAndOrderId(GATEWAY, ipnResult.getOrderId())
                .orElseGet(() -> {
                    log.warn("[MoMo IPN] Không tìm thấy payment request với orderId={}", ipnResult.getOrderId());
                    return null;
                });

        if (paymentReq == null) {
            // Có thể đây là callback cho đơn đã xử lý hoặc orderId không hợp lệ — bỏ qua an toàn
            return;
        }

        // 3. Parse bookingId từ orderId
        Long bookingId;
        try {
            String[] parts = ipnResult.getOrderId().split("_");
            bookingId = Long.parseLong(parts[0]);
        } catch (Exception e) {
            log.error("[MoMo IPN] Không parse được bookingId từ orderId={}", ipnResult.getOrderId());
            throw new BusinessException("orderId MoMo không hợp lệ", ErrorCode.MOMO_ORDER_ID_INVALID);
        }

        // 4. Cập nhật payment request
        paymentReq.setGatewayTransId(ipnResult.getTransactionId());
        paymentReq.setPayType(ipnResult.getPayType());

        if (ipnResult.isSuccess()) {
            // Thanh toán thành công
            paymentReq.setStatus(PaymentGatewayRequestStatus.PAID);
            paymentRequestService.save(paymentReq);

            String paymentRef = ipnResult.getTransactionId() != null ? ipnResult.getTransactionId() : ipn.getRequestId();
            bookingService.confirmPaymentByGateway(bookingId, paymentRef, GATEWAY);
            log.info("[MoMo IPN] Booking {} đã PAID thành công", bookingId);
        } else {
            // Thanh toán thất bại
            paymentReq.setStatus(PaymentGatewayRequestStatus.FAILED);
            paymentRequestService.save(paymentReq);
            log.warn("[MoMo IPN] Booking {} thanh toán thất bại, resultCode={}, message={}",
                    bookingId, ipnResult.getResultCode(), ipnResult.getMessage());
        }
    }
}
