package com.uit.backend_cinema.modules.payment.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.payment.api.dto.ClaimRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.api.dto.ConfirmCashPayoutResponseDTO;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceipt;
import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.infrastructure.client.VnpayMerchantClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(VN_ZONE);

    private final RefundRepository refundRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final VnpayMerchantClient vnpayMerchantClient;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Autowired
    public RefundService(
            RefundRepository refundRepository,
            PaymentReceiptRepository paymentReceiptRepository,
            @Autowired(required = false) VnpayMerchantClient vnpayMerchantClient,
            Clock clock,
            ObjectMapper objectMapper) {
        this.refundRepository = refundRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.vnpayMerchantClient = vnpayMerchantClient;
        this.clock = clock != null ? clock : Clock.system(VN_ZONE);
        this.objectMapper = objectMapper;
    }

    /**
     * Staff nhận xử lý độc quyền hoàn tiền mặt tại quầy (claim).
     */
    @Transactional
    public ClaimRefundResponseDTO claimCashRefund(Long refundId, Long staffId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException("Yêu cầu hoàn tiền không tồn tại: " + refundId, ErrorCode.REFUND_NOT_FOUND));

        PaymentReceipt receipt = paymentReceiptRepository.findById(refund.getPaymentReceiptId())
                .orElseThrow(() -> new BusinessException("Biên nhận thanh toán không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));

        if (!"CASH".equalsIgnoreCase(receipt.getGateway())) {
            throw new BusinessException("Chỉ được nhận xử lý tiền mặt cho giao dịch CASH", ErrorCode.REFUND_METHOD_MISMATCH);
        }

        if (refund.getStatus() == RefundStatus.SUCCEEDED) {
            throw new BusinessException("Yêu cầu hoàn tiền đã hoàn tất chi trả", ErrorCode.REFUND_INVALID_STATUS);
        }

        if (refund.getStatus() == RefundStatus.PROCESSING && refund.getActorStaffId() != null
                && !refund.getActorStaffId().equals(staffId)) {
            throw new BusinessException("Yêu cầu hoàn tiền này đang được xử lý bởi nhân viên khác (ID=" + refund.getActorStaffId() + ")",
                    ErrorCode.REFUND_ALREADY_CLAIMED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        boolean claimed = refundRepository.claimCashRefund(refundId, staffId, now);
        if (!claimed) {
            throw new BusinessException("Không thể nhận xử lý yêu cầu hoàn tiền (trạng thái đã thay đổi)", ErrorCode.REFUND_INVALID_STATUS);
        }

        log.info("[Refund CASH] Staff {} đã claim refund {} cho booking {}", staffId, refundId, refund.getBookingId());
        return ClaimRefundResponseDTO.builder()
                .refundId(refundId)
                .bookingId(refund.getBookingId())
                .status(RefundStatus.PROCESSING)
                .actorStaffId(staffId)
                .claimedAt(now)
                .amount(refund.getAmount())
                .message("Đã nhận xử lý hoàn tiền mặt thành công")
                .build();
    }

    /**
     * Xác nhận đã chi tiền mặt cho khách tại quầy.
     * Chỉ nhân viên đã claim mới được quyền xác nhận.
     */
    @Transactional
    public ConfirmCashPayoutResponseDTO confirmCashPayout(Long refundId, Long staffId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException("Yêu cầu hoàn tiền không tồn tại: " + refundId, ErrorCode.REFUND_NOT_FOUND));

        PaymentReceipt receipt = paymentReceiptRepository.findById(refund.getPaymentReceiptId())
                .orElseThrow(() -> new BusinessException("Biên nhận thanh toán không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));

        if (!"CASH".equalsIgnoreCase(receipt.getGateway())) {
            throw new BusinessException("Không thể chi trả tiền mặt cho giao dịch " + receipt.getGateway(), ErrorCode.REFUND_METHOD_MISMATCH);
        }

        if (refund.getStatus() == RefundStatus.SUCCEEDED) {
            return ConfirmCashPayoutResponseDTO.builder()
                    .refundId(refundId)
                    .bookingId(refund.getBookingId())
                    .status(RefundStatus.SUCCEEDED)
                    .actorStaffId(refund.getActorStaffId())
                    .processedAt(refund.getProcessedAt())
                    .amount(refund.getAmount())
                    .message("Yêu cầu hoàn tiền đã được xác nhận chi trả trước đó")
                    .build();
        }

        if (refund.getStatus() != RefundStatus.PROCESSING) {
            throw new BusinessException("Yêu cầu hoàn tiền chưa được nhận xử lý", ErrorCode.REFUND_INVALID_STATUS);
        }

        if (!staffId.equals(refund.getActorStaffId())) {
            throw new BusinessException("Chỉ nhân viên đã nhận xử lý mới có quyền xác nhận chi tiền", ErrorCode.REFUND_STAFF_MISMATCH);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        boolean confirmed = refundRepository.confirmCashPayout(refundId, staffId, now);
        if (!confirmed) {
            throw new BusinessException("Không thể xác nhận chi tiền (trạng thái đã thay đổi)", ErrorCode.REFUND_INVALID_STATUS);
        }

        log.info("[Refund CASH] Staff {} đã xác nhận chi tiền cho refund {} (số tiền={})", staffId, refundId, refund.getAmount());
        return ConfirmCashPayoutResponseDTO.builder()
                .refundId(refundId)
                .bookingId(refund.getBookingId())
                .status(RefundStatus.SUCCEEDED)
                .actorStaffId(staffId)
                .processedAt(now)
                .amount(refund.getAmount())
                .message("Đã xác nhận chi tiền mặt cho khách thành công")
                .build();
    }

    /**
     * Xử lý gửi yêu cầu hoàn tiền VNPay tự động ra cổng gateway ngoài DB transaction.
     */
    public void processOnlineRefund(Long refundId) {
        if (vnpayMerchantClient == null) {
            return;
        }

        Refund refund = refundRepository.findById(refundId).orElse(null);
        if (refund == null || refund.getStatus() != RefundStatus.REQUESTED) {
            return;
        }

        PaymentReceipt receipt = paymentReceiptRepository.findById(refund.getPaymentReceiptId()).orElse(null);
        if (receipt == null || !"VNPAY".equalsIgnoreCase(receipt.getGateway())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        // Chuyển sang PROCESSING để claim
        refund.setStatus(RefundStatus.PROCESSING);
        refund.setClaimedAt(now);
        refundRepository.save(refund);

        try {
            boolean isFull = refund.getAmount().compareTo(receipt.getAmount()) >= 0;
            String transactionType = isFull ? "02" : "03";
            String origDate = receipt.getPayDate() != null
                    ? receipt.getPayDate().format(DATE_FORMATTER)
                    : DATE_FORMATTER.format(now.atZone(VN_ZONE));

            VnpayRefundResponseDTO res = vnpayMerchantClient.refundTransaction(
                    receipt.getOrderId(),
                    refund.getAmount(),
                    transactionType,
                    receipt.getTransactionReference(),
                    origDate,
                    "system",
                    "Hoan tien don #" + refund.getBookingId(),
                    "127.0.0.1"
            );

            refund.setGatewayResponseCode(res.getResponseCode());
            refund.setGatewayTransactionNo(res.getTransactionNo());
            refund.setGatewayResponseMessage(res.getMessage());
            refund.setProcessedAt(now);

            if (res.isRefundSucceeded()) {
                refund.setStatus(RefundStatus.SUCCEEDED);
                log.info("[VNPay Refund] Refund {} thành công cho booking {}", refundId, refund.getBookingId());
            } else if (res.isRefundProcessing()) {
                refund.setStatus(RefundStatus.PROCESSING);
                log.info("[VNPay Refund] Refund {} đang được VNPay xử lý", refundId);
            } else {
                refund.setStatus(RefundStatus.FAILED);
                log.warn("[VNPay Refund] Refund {} bị từ chối: code={}, msg={}", refundId, res.getResponseCode(), res.getMessage());
            }
        } catch (BusinessException ex) {
            log.error("[VNPay Refund] Gọi cổng lỗi cho refund {}: {}", refundId, ex.getMessage());
            if (ex.getCode() == ErrorCode.GATEWAY_TIMEOUT) {
                // Timeout là unknown, không retry mù
                refund.setStatus(RefundStatus.UNKNOWN);
            } else {
                refund.setStatus(RefundStatus.REVIEW_REQUIRED);
            }
        } catch (Exception ex) {
            log.error("[VNPay Refund] Ngoại lệ khi refund {}: {}", refundId, ex.getMessage());
            refund.setStatus(RefundStatus.UNKNOWN);
        }

        refundRepository.save(refund);
    }

    /**
     * Worker định kỳ quét các yêu cầu hoàn tiền VNPay ở trạng thái REQUESTED.
     */
    @Scheduled(fixedDelayString = "${vnpay.refund-worker-interval-ms:60000}")
    public void scanAndProcessOnlineRefunds() {
        if (vnpayMerchantClient == null) {
            return;
        }

        List<Refund> requestedRefunds = refundRepository.findByStatus(RefundStatus.REQUESTED);
        for (Refund r : requestedRefunds) {
            try {
                PaymentReceipt receipt = paymentReceiptRepository.findById(r.getPaymentReceiptId()).orElse(null);
                if (receipt != null && "VNPAY".equalsIgnoreCase(receipt.getGateway())) {
                    processOnlineRefund(r.getRefundId());
                }
            } catch (Exception e) {
                log.error("[Refund Worker] Lỗi xử lý refund ID={}: {}", r.getRefundId(), e.getMessage());
            }
        }
    }
}
