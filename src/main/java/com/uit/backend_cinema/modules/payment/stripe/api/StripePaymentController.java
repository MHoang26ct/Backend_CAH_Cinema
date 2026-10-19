package com.uit.backend_cinema.modules.payment.stripe.api;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.payment.stripe.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripePaymentController {
    private final StripePaymentService payments;
    private final StripeWebhookService webhooks;
    public record CheckoutResponse(String payUrl, String sessionId, java.time.LocalDateTime paymentDeadline) {}

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/v1/bookings/{bookingId}/stripe/pay")
    public ResponseEntity<?> pay(@PathVariable long bookingId, @AuthenticationPrincipal CustomUserDetails user) {
        var p = payments.create(user.getUserId(), bookingId, "CHECKOUT", null);
        return ResponseEntity.ok(ApiResponse.success(new CheckoutResponse(p.payUrl(), p.sessionId(), p.deadline()), "Stripe Checkout created"));
    }
    @GetMapping(value = "/api/v1/public/stripe/return", produces = "text/plain;charset=UTF-8")
    public String returned() {
        return "Cinema Sandbox: quay lại ứng dụng để xem trạng thái booking. Trang này không xác nhận thanh toán.";
    }

    @PostMapping("/api/v1/public/stripe/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String raw,
                                       @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        webhooks.handle(raw, signature);
        return ResponseEntity.ok().build();
    }
}
