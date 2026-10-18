package com.uit.backend_cinema.modules.payment.stripe.api;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.modules.payment.stripe.config.StripeSandboxConfig;
import com.uit.backend_cinema.modules.payment.stripe.domain.StripePaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** API-level sandbox automation, NOT hosted Checkout UI E2E. No PANs or client secrets. */
@RestController
@RequiredArgsConstructor
@Profile("stripe-sandbox & !prod & !production")
@ConditionalOnProperty(name = {"stripe.enabled", "stripe.sandbox-driver-enabled"}, havingValue = "true")
public class StripeSandboxController {
    private static final Map<String, String> METHODS = Map.of(
            "SUCCESS", "pm_card_visa",
            "CARD_DECLINED", "pm_card_visa_chargeDeclined",
            "INSUFFICIENT_FUNDS", "pm_card_visa_chargeDeclinedInsufficientFunds",
            "THREE_D_SECURE", "pm_card_threeDSecure2Required");
    private final StripePaymentService payments;
    private final StripeSandboxConfig config;
    public record Scenario(@NotBlank String scenario) {}
    public record Result(String paymentIntentId, String status) {}

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/api/v1/bookings/{bookingId}/stripe/sandbox/payment-intent")
    public Result pay(@PathVariable long bookingId, @Valid @RequestBody Scenario scenario,
                      @RequestHeader(value = "X-Sandbox-Token", required = false) String token,
                      @AuthenticationPrincipal CustomUserDetails user) {
        if (token == null || config.getSandboxToken() == null || config.getSandboxToken().length() < 32 ||
                !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), config.getSandboxToken().getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sandbox token required");
        }
        String method = METHODS.get(scenario.scenario());
        if (method == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sandbox scenario");
        var p = payments.create(user.getUserId(), bookingId, "API", method);
        return new Result(p.intentId(), "AWAITING_WEBHOOK");
    }
}
