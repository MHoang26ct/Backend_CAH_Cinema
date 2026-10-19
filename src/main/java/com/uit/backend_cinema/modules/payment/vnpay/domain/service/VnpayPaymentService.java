package com.uit.backend_cinema.modules.payment.vnpay.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmation;
import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmationStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.PaymentConfirmationRepository;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.payload.BookingPaidPayload;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequestStatus;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentIpnResult;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceipt;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceiptStatus;
import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundCause;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import com.uit.backend_cinema.modules.payment.domain.service.PaymentRequestService;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayIpnResponse;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayPayRequest;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayPayResponse;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayQueryResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.domain.config.VnpayPaymentConfig;
import com.uit.backend_cinema.modules.payment.vnpay.domain.repository.VnpayPaymentRepository;
import com.uit.backend_cinema.modules.payment.vnpay.infrastructure.client.VnpayMerchantClient;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class VnpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentService.class);
    private static final String GATEWAY = "VNPAY";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(VN_ZONE);

    private final VnpayPaymentConfig vnpayConfig;
    private final VnpayPaymentRepository vnpayPaymentRepository;
    private final PaymentRequestService paymentRequestService;
    private final BookingRepository bookingRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final RefundRepository refundRepository;
    private final PaymentConfirmationRepository paymentConfirmationRepository;
    private final TicketService ticketService;
    private final SeatService seatService;
    private final FoodOrderService foodOrderService;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final VnpayMerchantClient vnpayMerchantClient;

    @Autowired
    public VnpayPaymentService(
            VnpayPaymentConfig vnpayConfig,
            VnpayPaymentRepository vnpayPaymentRepository,
            PaymentRequestService paymentRequestService,
            BookingRepository bookingRepository,
            PaymentReceiptRepository paymentReceiptRepository,
            RefundRepository refundRepository,
            PaymentConfirmationRepository paymentConfirmationRepository,
            TicketService ticketService,
            SeatService seatService,
            FoodOrderService foodOrderService,
            OutboxEventService outboxEventService,
            ObjectMapper objectMapper,
            Clock clock,
            @Autowired(required = false) VnpayMerchantClient vnpayMerchantClient) {
        this.vnpayConfig = vnpayConfig;
        this.vnpayPaymentRepository = vnpayPaymentRepository;
        this.paymentRequestService = paymentRequestService;
        this.bookingRepository = bookingRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.refundRepository = refundRepository;
        this.paymentConfirmationRepository = paymentConfirmationRepository;
        this.ticketService = ticketService;
        this.seatService = seatService;
        this.foodOrderService = foodOrderService;
        this.outboxEventService = outboxEventService;
        this.objectMapper = objectMapper;
        this.clock = clock != null ? clock : Clock.system(VN_ZONE);
        this.vnpayMerchantClient = vnpayMerchantClient;
    }

    /**
     * Tạo URL thanh toán VNPay cho booking.
     * Sử dụng paymentDeadline của booking; không tạo mới sau hạn và không kéo dài bằng retry.
     */
    @Transactional
    public VnpayPayResponse createVnpayPayment(Long userId, boolean isStaffOrAdmin, Long bookingId, VnpayPayRequest payRequest, String ipAddr) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));

        if (!booking.getUserId().equals(userId) && !isStaffOrAdmin) {
            throw new BusinessException("Bạn không có quyền thanh toán booking này", ErrorCode.FORBIDDEN);
        }
        if (booking.getPaymentMethod() != BookingPaymentMethod.VNPAY) {
            throw new BusinessException("Booking này không dùng phương thức thanh toán VNPay", ErrorCode.BOOKING_INVALID_STATUS);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Booking không ở trạng thái chờ thanh toán", ErrorCode.BOOKING_INVALID_STATUS);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime paymentDeadline = booking.getPaymentDeadline() != null ? booking.getPaymentDeadline() : booking.getExpiresAt();
        if (paymentDeadline != null && !paymentDeadline.isAfter(now)) {
            throw new BusinessException("Booking đã hết hạn thanh toán", ErrorCode.BOOKING_EXPIRED);
        }

        // Lọc latest request theo đúng gateway và booking
        Optional<PaymentGatewayRequest> existingOpt = paymentRequestService.findLatestByBookingIdAndGateway(bookingId, GATEWAY);

        if (existingOpt.isPresent()) {
            PaymentGatewayRequest ex = existingOpt.get();
            if (ex.getStatus() == PaymentGatewayRequestStatus.CREATED) {
                boolean isExpiredByDeadline = paymentDeadline != null && !paymentDeadline.isAfter(now);
                boolean isExpiredByTtl = ex.getCreatedAt().isBefore(now.minusMinutes(vnpayConfig.getPaymentRequestTtlMinutes()));

                if (!isExpiredByDeadline && !isExpiredByTtl) {
                    log.info("[VNPay] Booking {} đã có payment request CREATED còn hiệu lực (requestId={}), trả lại URL cũ",
                            bookingId, ex.getRequestId());
                    return VnpayPayResponse.builder()
                            .payUrl(ex.getPayUrl())
                            .vnpayOrderId(ex.getOrderId())
                            .build();
                }

                log.info("[VNPay] Booking {} có payment request CREATED nhưng đã hết hạn, cập nhật status FAILED", bookingId);
                ex.setStatus(PaymentGatewayRequestStatus.FAILED);
                ex.setResponseMessage("Hết hạn thanh toán");
                paymentRequestService.save(ex);
            }
        }

        String orderId = bookingId.toString() + "_" + System.currentTimeMillis();
        String clientRequestId = payRequest.getRequestId();
        String gatewayRequestId = UUID.randomUUID().toString();
        long amount = booking.getTotalAmount().longValue();
        String orderInfo = "Thanh toan booking #" + bookingId;

        String vnpCreateDate = DATE_FORMATTER.format(now.atZone(VN_ZONE));
        String vnpExpireDate = paymentDeadline != null
                ? DATE_FORMATTER.format(paymentDeadline.atZone(VN_ZONE))
                : DATE_FORMATTER.format(now.plusMinutes(vnpayConfig.getPaymentRequestTtlMinutes()).atZone(VN_ZONE));

        PaymentGatewayRequest request = PaymentGatewayRequest.builder()
                .bookingId(bookingId)
                .gateway(GATEWAY)
                .requestId(clientRequestId)
                .gatewayRequestId(gatewayRequestId)
                .orderId(orderId)
                .amount(amount)
                .orderInfo(orderInfo)
                .vnpCreateDate(vnpCreateDate)
                .vnpExpireDate(vnpExpireDate)
                .build();

        PaymentGatewayRequest result = vnpayPaymentRepository.createVnpayPayment(
                request,
                ipAddr,
                null
        );

        paymentRequestService.save(result);

        if (result.getResultCode() != 0) {
            log.error("[VNPay] Tạo URL thanh toán thất bại cho booking {}, message={}", bookingId, result.getResponseMessage());
            throw new BusinessException("Tạo đơn VNPay thất bại: " + result.getResponseMessage(), ErrorCode.VNPAY_PAYMENT_CREATION_FAILED);
        }

        log.info("[VNPay] Tạo URL thanh toán thành công cho booking {}, orderId={}, vnpExpireDate={}", bookingId, orderId, vnpExpireDate);
        return VnpayPayResponse.builder()
                .payUrl(result.getPayUrl())
                .vnpayOrderId(orderId)
                .build();
    }

    /**
     * Xử lý IPN Callback từ VNPay.
     * Luôn trả về VnpayIpnResponse với mã RspCode tương ứng để VNPay ACK.
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

        // Cập nhật metadata từ gateway
        paymentReq.setGatewayTransId(ipnResult.getTransactionId());
        paymentReq.setPayType(ipnResult.getPayType());
        paymentReq.setVnpPayDate(ipnResult.getPayDate());
        paymentReq.setVnpBankCode(ipnResult.getBankCode());
        paymentReq.setVnpBankTranNo(ipnResult.getBankTranNo());
        paymentReq.setVnpCardType(ipnResult.getCardType());

        LocalDateTime now = LocalDateTime.now(clock);

        if (!ipnResult.isSuccess()) {
            log.warn("[VNPay IPN] Thanh toán thất bại tại gateway cho orderId={}, resultCode={}",
                    ipnResult.getOrderId(), ipnResult.getResultCode());
            if (paymentReq.getStatus() == PaymentGatewayRequestStatus.CREATED) {
                paymentReq.setStatus(PaymentGatewayRequestStatus.FAILED);
                paymentReq.setResponseMessage("Thanh toán thất bại tại gateway: Code=" + ipnResult.getResultCode());
                paymentRequestService.save(paymentReq);
            }
            return VnpayIpnResponse.builder().rspCode("00").message("Confirm Success").build();
        }

        // Tiền đã thanh toán thành công tại gateway
        processSuccessfulPayment(
                paymentReq,
                ipnResult.getTransactionId(),
                ipnResult.getPayDate(),
                ipnResult.getBankCode(),
                ipnResult.getBankTranNo(),
                ipnResult.getCardType(),
                now
        );

        return VnpayIpnResponse.builder().rspCode("00").message("Confirm Success").build();
    }

    /**
     * Đối soát tự động định kỳ cho các yêu cầu VNPay đang chờ.
     */
    @Scheduled(fixedDelayString = "${vnpay.reconciliation-interval-ms:30000}")
    @Transactional
    public void reconcilePendingPayments() {
        if (vnpayMerchantClient == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime threshold = now.minusSeconds(30);
        List<PaymentGatewayRequest> pendingRequests = paymentRequestService.findPendingReconciliationRequests(GATEWAY, threshold);

        for (PaymentGatewayRequest req : pendingRequests) {
            try {
                String origDate = req.getVnpCreateDate() != null ? req.getVnpCreateDate() : req.getCreatedAt().format(DATE_FORMATTER);
                VnpayQueryResponseDTO queryResult = vnpayMerchantClient.queryTransaction(
                        req.getOrderId(),
                        origDate,
                        req.getOrderInfo(),
                        "127.0.0.1"
                );

                if (queryResult.isPaymentSuccess()) {
                    processSuccessfulPayment(
                            req,
                            queryResult.getTransactionNo(),
                            queryResult.getPayDate(),
                            queryResult.getBankCode(),
                            null,
                            null,
                            now
                    );
                } else if ("02".equals(queryResult.getTransactionStatus()) || "02".equals(queryResult.getResponseCode())) {
                    req.setStatus(PaymentGatewayRequestStatus.FAILED);
                    req.setResponseMessage("Giao dịch thất bại tại gateway: " + queryResult.getMessage());
                    paymentRequestService.save(req);
                }
            } catch (BusinessException ex) {
                log.warn("[VNPay Reconciliation] Giao dịch {} đối soát có lỗi: {}", req.getOrderId(), ex.getMessage());
            } catch (Exception ex) {
                log.error("[VNPay Reconciliation] Lỗi đối soát giao dịch {}: {}", req.getOrderId(), ex.getMessage());
            }
        }
    }

    /**
     * Ghi nhận khoản thu bền vững và chốt vé hoặc tạo nghĩa vụ hoàn tiền tương ứng.
     */
    private void processSuccessfulPayment(
            PaymentGatewayRequest paymentReq,
            String paymentRef,
            String vnpPayDate,
            String bankCode,
            String bankTranNo,
            String cardType,
            LocalDateTime now) {

        Long bookingId = paymentReq.getBookingId();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null) {
            paymentReq.setStatus(PaymentGatewayRequestStatus.FAILED);
            paymentReq.setResponseMessage("Booking không tồn tại: " + bookingId);
            paymentRequestService.save(paymentReq);
            return;
        }

        LocalDateTime payDate = parseVnpDate(vnpPayDate, now);

        // 1. Ghi nhận PaymentReceipt bền vững
        PaymentReceipt receipt = null;
        if (paymentReceiptRepository != null) {
            Optional<PaymentReceipt> existingReceipt = paymentReceiptRepository.findByGatewayAndTransactionReference(GATEWAY, paymentRef);
            if (existingReceipt.isPresent()) {
                receipt = existingReceipt.get();
            } else {
                receipt = PaymentReceipt.builder()
                        .bookingId(bookingId)
                        .paymentRequestId(paymentReq.getPaymentRequestId())
                        .gateway(GATEWAY)
                        .transactionReference(paymentRef)
                        .orderId(paymentReq.getOrderId())
                        .amount(booking.getTotalAmount())
                        .payDate(payDate)
                        .verifiedAt(now)
                        .status(PaymentReceiptStatus.RECORDED)
                        .build();
                try {
                    receipt = paymentReceiptRepository.save(receipt);
                } catch (Exception ex) {
                    receipt = paymentReceiptRepository.findByGatewayAndTransactionReference(GATEWAY, paymentRef).orElse(receipt);
                }
            }
        }

        // 2. Kiểm tra Idempotency cho xác nhận đặt vé
        if (paymentConfirmationRepository != null) {
            Optional<PaymentConfirmation> existingConf = paymentConfirmationRepository.findByPaymentRef(paymentRef);
            if (existingConf.isPresent()) {
                if (paymentReq.getStatus() != PaymentGatewayRequestStatus.PAID) {
                    paymentReq.setStatus(PaymentGatewayRequestStatus.PAID);
                    paymentRequestService.save(paymentReq);
                }
                return;
            }
        }

        // 3. Phán quyết trạng thái booking
        LocalDateTime paymentDeadline = booking.getPaymentDeadline() != null ? booking.getPaymentDeadline() : booking.getExpiresAt();
        boolean isPending = booking.getStatus() == BookingStatus.PENDING;
        boolean onTime = paymentDeadline == null || !payDate.isAfter(paymentDeadline);
        boolean seatsAvailable = false;
        List<Long> seatIds = List.of();
        if (isPending) {
            try {
                seatIds = ticketService != null ? ticketService.findActiveDraftSeatIds(bookingId) : List.of();
                if (seatService != null) {
                    seatService.validateSeatsNotSold(booking.getShowtimeId(), seatIds);
                }
                seatsAvailable = true;
            } catch (Exception ex) {
                seatsAvailable = false;
            }
        }

        if (isPending && onTime && seatsAvailable) {
            if (foodOrderService != null) {
                foodOrderService.finalizeDraftForBookingIfAbsent(bookingId);
            }
            booking.setStatus(BookingStatus.PAID);
            bookingRepository.save(booking);

            if (ticketService != null) {
                try {
                    ticketService.finalizeTicketsForPaidBooking(bookingId, booking.getShowtimeId());
                } catch (DataIntegrityViolationException ex) {
                    createRefundObligation(receipt, booking, RefundCause.SEAT_ALLOCATION_FAILED, "Ghế đã được bán cho suất chiếu này", now);
                    paymentReq.setStatus(PaymentGatewayRequestStatus.PAID);
                    paymentReq.setResponseMessage("Thu tiền thành công nhưng ghế đã được bán; tạo nghĩa vụ hoàn tiền");
                    paymentRequestService.save(paymentReq);
                    return;
                }
            }

            if (paymentConfirmationRepository != null) {
                PaymentConfirmation confirmation = new PaymentConfirmation();
                confirmation.setBookingId(bookingId);
                confirmation.setPaymentRef(paymentRef);
                confirmation.setStatus(PaymentConfirmationStatus.SUCCESS);
                confirmation.setGateway(GATEWAY);
                try {
                    paymentConfirmationRepository.save(confirmation);
                } catch (Exception ex) {
                    // Ignored if duplicate
                }
            }

            createBookingPaidOutbox(booking, paymentRef);
            paymentReq.setStatus(PaymentGatewayRequestStatus.PAID);
            paymentReq.setResponseMessage("Thanh toán thành công qua VNPay");
            paymentRequestService.save(paymentReq);
            log.info("[VNPay] Booking {} chốt vé thành công với paymentRef={}", bookingId, paymentRef);
        } else {
            RefundCause cause;
            String reason;
            if (booking.getStatus() == BookingStatus.PAID) {
                cause = RefundCause.DUPLICATE_CHARGE;
                reason = "Booking đã được thanh toán trước đó; giao dịch trùng được ghi nhận để hoàn tiền";
            } else if (!onTime || booking.getStatus() == BookingStatus.EXPIRED) {
                cause = RefundCause.EXPIRED_LATE_PAYMENT;
                reason = "Thanh toán nhận được sau thời hạn booking; tạo nghĩa vụ hoàn tiền 100%";
            } else {
                cause = RefundCause.SEAT_ALLOCATION_FAILED;
                reason = "Không thể cấp vé do ghế không còn khả dụng; tạo nghĩa vụ hoàn tiền 100%";
            }

            createRefundObligation(receipt, booking, cause, reason, now);
            paymentReq.setStatus(PaymentGatewayRequestStatus.PAID);
            paymentReq.setResponseMessage(reason);
            paymentRequestService.save(paymentReq);
            log.warn("[VNPay] Booking {} không thể chốt vé ({}). Đã ghi nhận khoản thu và tạo nghĩa vụ hoàn {}",
                    bookingId, reason, cause);
        }
    }

    private void createRefundObligation(PaymentReceipt receipt, Booking booking, RefundCause cause, String reason, LocalDateTime now) {
        if (refundRepository == null || receipt == null || receipt.getPaymentReceiptId() == null) {
            return;
        }
        Optional<Refund> existing = refundRepository.findByPaymentReceiptIdAndCause(receipt.getPaymentReceiptId(), cause);
        if (existing.isPresent()) {
            return;
        }
        Refund refund = Refund.builder()
                .paymentReceiptId(receipt.getPaymentReceiptId())
                .bookingId(booking.getBookingId())
                .cause(cause)
                .amount(receipt.getAmount())
                .policySnapshot("{\"reason\":\"" + reason + "\"}")
                .status(RefundStatus.REQUESTED)
                .requestReference("REFUND_AUTO_" + receipt.getPaymentReceiptId())
                .createdAt(now)
                .build();
        try {
            refundRepository.save(refund);
        } catch (Exception ex) {
            log.warn("[VNPay] Nghĩa vụ hoàn tiền cho receipt {} đã tồn tại", receipt.getPaymentReceiptId());
        }
    }

    private void createBookingPaidOutbox(Booking booking, String paymentRef) {
        if (outboxEventService == null || objectMapper == null) {
            return;
        }
        BookingPaidPayload payload = new BookingPaidPayload();
        payload.setBookingId(booking.getBookingId());
        payload.setUserId(booking.getUserId());
        payload.setShowtimeId(booking.getShowtimeId());
        payload.setPaymentRef(paymentRef);

        try {
            outboxEventService.createIfAbsent(
                    OutboxEventType.BOOKING_PAID,
                    booking.getBookingId().toString(),
                    objectMapper.writeValueAsString(payload)
            );
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Không thể tạo outbox event", ErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, ex);
        }
    }

    private LocalDateTime parseVnpDate(String vnpPayDate, LocalDateTime fallback) {
        if (vnpPayDate == null || vnpPayDate.length() < 14) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(vnpPayDate.substring(0, 14), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            return fallback;
        }
    }
}
