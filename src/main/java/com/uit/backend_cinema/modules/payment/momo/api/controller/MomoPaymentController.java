package com.uit.backend_cinema.modules.payment.momo.api.controller;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.payment.momo.api.dto.MomoIpnRequest;
import com.uit.backend_cinema.modules.payment.momo.api.dto.MomoPayRequest;
import com.uit.backend_cinema.modules.payment.momo.api.dto.MomoPayResponse;
import com.uit.backend_cinema.modules.payment.momo.domain.service.MomoPaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class MomoPaymentController {

    private static final Logger log = LoggerFactory.getLogger(MomoPaymentController.class);

    private final MomoPaymentService momoPaymentService;

    public MomoPaymentController(MomoPaymentService momoPaymentService) {
        this.momoPaymentService = momoPaymentService;
    }

    /**
     * Tạo đơn thanh toán MoMo cho booking.
     * Client cần tạo và truyền requestId (UUID) — dùng làm idempotency key.
     *
     * POST /api/v1/bookings/{bookingId}/momo/pay
     * Auth: JWT required
     */
    @PostMapping("/api/v1/bookings/{bookingId}/momo/pay")
    public ResponseEntity<?> createMomoPayment(
            @PathVariable Long bookingId,
            @Valid @RequestBody MomoPayRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        boolean isStaffOrAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"));

        MomoPayResponse response = momoPaymentService.createMomoPayment(user.getUserId(), user.getUsername(), isStaffOrAdmin, bookingId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo đơn thanh toán MoMo thành công"));
    }

    /**
     * IPN callback từ MoMo — gọi server-to-server sau khi user thanh toán.
     * MoMo yêu cầu phản hồi HTTP 204 (không có body).
     * Endpoint public vì MoMo server gọi trực tiếp (không qua JWT).
     *
     * POST /api/v1/public/momo/ipn
     */
    @PostMapping("/api/v1/public/momo/ipn")
    public ResponseEntity<Void> handleIpn(@RequestBody MomoIpnRequest ipnRequest) {
        log.info("[MoMo IPN] Nhận callback: orderId={}, resultCode={}", ipnRequest.getOrderId(), ipnRequest.getResultCode());
        try {
            momoPaymentService.handleIpnCallback(ipnRequest);
        } catch (Exception e) {
            // Luôn trả 204 cho MoMo dù có lỗi — log để debug
            log.error("[MoMo IPN] Lỗi xử lý callback: orderId={}, error={}", ipnRequest.getOrderId(), e.getMessage());
        }
        return ResponseEntity.noContent().build(); // HTTP 204
    }
}
