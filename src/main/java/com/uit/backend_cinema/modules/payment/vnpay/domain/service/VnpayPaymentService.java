package com.uit.backend_cinema.modules.payment.vnpay.domain.service;

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
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayIpnResponse;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayPayRequest;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayPayResponse;
import com.uit.backend_cinema.modules.payment.vnpay.domain.config.VnpayPaymentConfig;
import com.uit.backend_cinema.modules.payment.vnpay.domain.repository.VnpayPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class VnpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentService.class);
    private static final String GATEWAY = "VNPAY";

    private final VnpayPaymentConfig vnpayConfig;
    private final VnpayPaymentRepository vnpayPaymentRepository;
    private final PaymentRequestService paymentRequestService;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public VnpayPaymentService(VnpayPaymentConfig vnpayConfig,
                               VnpayPaymentRepository vnpayPaymentRepository,
                               PaymentRequestService paymentRequestService,
                               BookingRepository bookingRepository,
                               BookingService bookingService) {
        this.vnpayConfig = vnpayConfig;
        this.vnpayPaymentRepository = vnpayPaymentRepository;
        this.paymentRequestService = paymentRequestService;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    /**
     * Tạo URL thanh toán VNPay cho booking.
     * Nếu booking đã có payment request CREATED và chưa hết TTL -> Trả lại URL cũ.
     */
    @Transactional
    public VnpayPayResponse createVnpayPayment(Long userId, boolean isStaffOrAdmin, Long bookingId, VnpayPayRequest payRequest, String ipAddr) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));

        // Validation
        if (!booking.getUserId().equals(userId) && !isStaffOrAdmin) {
            throw new BusinessException("Bạn không có quyền thanh toán booking này", ErrorCode.FORBIDDEN);
        }
        if (booking.getPaymentMethod() != BookingPaymentMethod.VNPAY) {
            throw new BusinessException("Booking này không dùng phương thức thanh toán VNPay", ErrorCode.BOOKING_INVALID_STATUS);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Booking không ở trạng thái chờ thanh toán", ErrorCode.BOOKING_INVALID_STATUS);
        }
        if (!booking.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Booking đã hết hạn thanh toán", ErrorCode.BOOKING_EXPIRED);
        }

        // Kiểm tra payment request hiện tại
        Optional<PaymentGatewayRequest> existingOpt = paymentRequestService.findLatestByBookingId(bookingId);

        if (existingOpt.isPresent()) {
            PaymentGatewayRequest ex = existingOpt.get();

            if (ex.getStatus() == PaymentGatewayRequestStatus.CREATED) {
                boolean isExpired = ex.getCreatedAt()
                        .isBefore(LocalDateTime.now().minusMinutes(vnpayConfig.getPaymentRequestTtlMinutes()));

                if (!isExpired) {
                    log.info("[VNPay] Booking {} đã có payment request CREATED còn hiệu lực (requestId={}), trả lại URL cũ",
                            bookingId, ex.getRequestId());
                    return VnpayPayResponse.builder()
                            .payUrl(ex.getPayUrl())
                            .vnpayOrderId(ex.getOrderId())
                            .build();
                }

                log.info("[VNPay] Booking {} có payment request CREATED nhưng đã hết TTL, cập nhật status FAILED", bookingId);
                ex.setStatus(PaymentGatewayRequestStatus.FAILED);
                ex.setResponseMessage("Hết hạn thanh toán sau " + vnpayConfig.getPaymentRequestTtlMinutes() + " phút");
                paymentRequestService.save(ex);
            }
        }

        // Sinh thông tin đơn hàng mới
        String orderId = bookingId.toString() + "_" + System.currentTimeMillis();
        String clientRequestId = payRequest.getRequestId();
        String gatewayRequestId = UUID.randomUUID().toString();
        long amount = booking.getTotalAmount().longValue();
        String orderInfo = "Thanh toan booking #" + bookingId;

        // Xây dựng domain request object
        PaymentGatewayRequest request = PaymentGatewayRequest.builder()
                .bookingId(bookingId)
                .gateway(GATEWAY)
                .requestId(clientRequestId)
                .gatewayRequestId(gatewayRequestId)
                .orderId(orderId)
                .amount(amount)
                .orderInfo(orderInfo)
                .build();

        // Gọi repo để xây dựng redirect URL
        PaymentGatewayRequest result = vnpayPaymentRepository.createVnpayPayment(
                request, 
                ipAddr, 
                null
        );

        // Lưu thông tin request
        paymentRequestService.save(result);

        if (result.getResultCode() != 0) {
            log.error("[VNPay] Tạo URL thanh toán thất bại cho booking {}, message={}", bookingId, result.getResponseMessage());
            throw new BusinessException("Tạo đơn VNPay thất bại: " + result.getResponseMessage(), ErrorCode.VNPAY_PAYMENT_CREATION_FAILED);
        }

        log.info("[VNPay] Tạo URL thanh toán thành công cho booking {}, orderId={}", bookingId, orderId);
        return VnpayPayResponse.builder()
                .payUrl(result.getPayUrl())
                .vnpayOrderId(orderId)
                .build();
    }

    /**
     * Xử lý IPN Callback từ VNPay.
     * Luôn trả về VnpayIpnResponse với mã RspCode tương ứng để VNPay không gọi lại IPN nữa.
     */
    @Transactional
    public VnpayIpnResponse handleIpnCallback(Map<String, String> params) {
        log.info("[VNPay IPN] Nhận callback với txnRef={}", params.get("vnp_TxnRef"));

        // 1. Xác thực và parse IPN
        PaymentIpnResult ipnResult = vnpayPaymentRepository.handleIpn(params);
        if (ipnResult == null) {
            log.warn("[VNPay IPN] Chữ ký không hợp lệ");
            return VnpayIpnResponse.builder().rspCode("97").message("Invalid signature").build();
        }

        // 2. Tìm payment request tương ứng
        Optional<PaymentGatewayRequest> paymentReqOpt = paymentRequestService
                .findByGatewayAndOrderId(GATEWAY, ipnResult.getOrderId());

        if (paymentReqOpt.isEmpty()) {
            log.warn("[VNPay IPN] Không tìm thấy payment request cho orderId={}", ipnResult.getOrderId());
            return VnpayIpnResponse.builder().rspCode("01").message("Order not found").build();
        }

        PaymentGatewayRequest paymentReq = paymentReqOpt.get();

        // 3. Kiểm tra số tiền (vnp_Amount đã nhân 100)
        String vnpAmountStr = params.get("vnp_Amount");
        long vnpAmountVal = 0;
        try {
            if (vnpAmountStr != null) {
                vnpAmountVal = Long.parseLong(vnpAmountStr);
            }
        } catch (NumberFormatException e) {
            log.warn("[VNPay IPN] Định dạng số tiền vnp_Amount không hợp lệ: {}", vnpAmountStr);
            return VnpayIpnResponse.builder().rspCode("04").message("Invalid amount").build();
        }

        if (vnpAmountVal != paymentReq.getAmount() * 100) {
            log.warn("[VNPay IPN] Số tiền không khớp. DB={}, VNPay={}", paymentReq.getAmount() * 100, vnpAmountVal);
            return VnpayIpnResponse.builder().rspCode("04").message("Invalid amount").build();
        }

        // 4. Kiểm tra trạng thái đơn hàng (tránh ghi đè các đơn hàng đã được cập nhật trước đó)
        if (paymentReq.getStatus() != PaymentGatewayRequestStatus.CREATED) {
            log.info("[VNPay IPN] Đơn hàng đã được xác nhận trước đó (status={})", paymentReq.getStatus());
            return VnpayIpnResponse.builder().rspCode("02").message("Order already confirmed").build();
        }

        // 5. Giải mã bookingId từ orderId (format: bookingId_timestamp)
        Long bookingId;
        try {
            String[] parts = ipnResult.getOrderId().split("_");
            bookingId = Long.parseLong(parts[0]);
        } catch (Exception e) {
            log.error("[VNPay IPN] Không parse được bookingId từ orderId={}", ipnResult.getOrderId());
            return VnpayIpnResponse.builder().rspCode("01").message("Order not found").build();
        }

        // 6. Cập nhật kết quả thanh toán
        paymentReq.setGatewayTransId(ipnResult.getTransactionId());
        paymentReq.setPayType(ipnResult.getPayType());

        if (ipnResult.isSuccess()) {
            paymentReq.setStatus(PaymentGatewayRequestStatus.PAID);
            paymentReq.setResponseMessage("Thanh toán thành công qua VNPay");
            paymentRequestService.save(paymentReq);

            // Xác nhận đặt vé thành công
            bookingService.confirmPaymentByGateway(bookingId, ipnResult.getTransactionId(), GATEWAY);
            log.info("[VNPay IPN] Booking {} đã được thanh toán thành công", bookingId);
        } else {
            paymentReq.setStatus(PaymentGatewayRequestStatus.FAILED);
            paymentReq.setResponseMessage("Thanh toán thất bại: Code=" + ipnResult.getResultCode());
            paymentRequestService.save(paymentReq);
            log.warn("[VNPay IPN] Booking {} thanh toán thất bại tại gateway, resultCode={}", bookingId, ipnResult.getResultCode());
        }

        return VnpayIpnResponse.builder().rspCode("00").message("Confirm Success").build();
    }
}
