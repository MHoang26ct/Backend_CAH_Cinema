package com.uit.backend_cinema.modules.payment.stripe.domain;

import java.time.LocalDateTime;

public record StripePayment(long bookingId, long requestId, String mode, long amount, String currency,
                            String accountId, String idempotencyKey, String sessionId, String intentId,
                            String payUrl, String state, LocalDateTime deadline, LocalDateTime createdAt,
                            String successUrl, String cancelUrl, String scenario) {
}
