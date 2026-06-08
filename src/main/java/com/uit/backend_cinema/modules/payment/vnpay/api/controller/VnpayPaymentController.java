package com.uit.backend_cinema.modules.payment.vnpay.api.controller;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayIpnResponse;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayPayRequest;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayPayResponse;
import com.uit.backend_cinema.modules.payment.vnpay.domain.service.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class VnpayPaymentController {

    private static final Logger log = LoggerFactory.getLogger(VnpayPaymentController.class);

    private final VnpayPaymentService vnpayPaymentService;

    public VnpayPaymentController(VnpayPaymentService vnpayPaymentService) {
        this.vnpayPaymentService = vnpayPaymentService;
    }

    /**
     * Tạo URL thanh toán VNPay cho booking.
     * Client cần truyền requestId (UUID) làm idempotency key và bankCode tùy chọn.
     * 
     * POST /api/v1/bookings/{bookingId}/vnpay/pay
     * Auth: JWT required
     */
    @PostMapping("/api/v1/bookings/{bookingId}/vnpay/pay")
    public ResponseEntity<?> createVnpayPayment(
            @PathVariable Long bookingId,
            @Valid @RequestBody VnpayPayRequest request,
            @AuthenticationPrincipal CustomUserDetails user,
            HttpServletRequest servletRequest) {

        String ipAddr = getClientIp(servletRequest);
        log.info("[VNPay Pay] Yêu cầu tạo thanh toán: bookingId={}, userId={}, ipAddr={}",
                bookingId, user.getUserId(), ipAddr);

        boolean isStaffOrAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"));

        VnpayPayResponse response = vnpayPaymentService.createVnpayPayment(
                user.getUserId(), 
                isStaffOrAdmin,
                bookingId, 
                request, 
                ipAddr
        );
        
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo đơn thanh toán VNPay thành công"));
    }

    /**
     * IPN Callback từ VNPay — gọi kiểu server-to-server sau khi giao dịch hoàn tất.
     * VNPay truyền tham số qua query string (GET).
     * Endpoint public không yêu cầu JWT.
     * 
     * GET /api/v1/public/vnpay/ipn
     */
    @GetMapping("/api/v1/public/vnpay/ipn")
    public ResponseEntity<VnpayIpnResponse> handleIpn(@RequestParam Map<String, String> params) {
        log.info("[VNPay IPN] Nhận callback chứa txnRef={}", params.get("vnp_TxnRef"));
        
        try {
            VnpayIpnResponse response = vnpayPaymentService.handleIpnCallback(params);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[VNPay IPN] Lỗi nghiêm trọng khi xử lý callback: {}", e.getMessage(), e);
            // Trả về lỗi 99 theo tài liệu hướng dẫn VNPay
            return ResponseEntity.ok(VnpayIpnResponse.builder()
                    .rspCode("99")
                    .message("Unknown error")
                    .build());
        }
    }

    /**
     * Lấy IP Address của client gửi request.
     */
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // Trích xuất IP đầu tiên nếu đi qua nhiều proxy
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        // Convert IPv6 localhost sang IPv4 localhost
        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }
}
