package com.uit.backend_cinema.modules.payment.stripe.domain;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.booking.domain.entity.*;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.payment.domain.entity.*;
import com.uit.backend_cinema.modules.payment.domain.service.PaymentRequestService;
import com.uit.backend_cinema.modules.payment.stripe.config.StripeSandboxConfig;
import com.uit.backend_cinema.modules.payment.stripe.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripePaymentService {
    public static final Set<String> SCENARIOS = Set.of("pm_card_visa", "pm_card_visa_chargeDeclined", "pm_card_visa_chargeDeclinedInsufficientFunds", "pm_card_threeDSecure2Required");
    private final BookingRepository bookings;
    private final PaymentRequestService requests;
    private final StripePaymentStore store;
    private final StripeGateway gateway;
    private final StripeSandboxConfig config;
    private final TransactionTemplate tx;
    private final Clock clock;

    public static long vnd(BigDecimal value) {
        long amount = value.longValueExact(); // VND is zero decimal, never multiply by 100.
        if (amount <= 0) throw new IllegalArgumentException("Stripe amount must be positive integer VND");
        return amount;
    }
    public LocalDateTime now() { return LocalDateTime.now(clock); }

    public StripePayment reserve(long userId, long bookingId, String mode, String scenario) {
        if (!Set.of("CHECKOUT", "API").contains(mode) || ("API".equals(mode) && (scenario == null || !SCENARIOS.contains(scenario)))) {
            throw new BusinessException("Unsupported sandbox scenario", ErrorCode.VALIDATION_FAILED);
        }
        return tx.execute(status -> {
            Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow(() ->
                    new BusinessException("Booking not found", ErrorCode.RESOURCE_NOT_FOUND));
            if (!booking.getUserId().equals(userId)) throw new BusinessException("Booking owner required", ErrorCode.FORBIDDEN);
            if (booking.getPaymentMethod() != BookingPaymentMethod.STRIPE || booking.getStatus() != BookingStatus.PENDING)
                throw new BusinessException("Booking not pending Stripe payment", ErrorCode.BOOKING_INVALID_STATUS);
            LocalDateTime deadline = booking.getPaymentDeadline() != null ? booking.getPaymentDeadline() : booking.getExpiresAt();
            if (deadline == null || !deadline.isAfter(now())) throw new BusinessException("Booking expired", ErrorCode.BOOKING_EXPIRED);
            var existing = store.find(bookingId);
            if (existing.isPresent()) {
                StripePayment p = existing.get();
                if (!mode.equals(p.mode()) || !java.util.Objects.equals(scenario, p.scenario()))
                    throw new BusinessException("Payment mode/scenario already reserved", ErrorCode.BOOKING_INVALID_STATUS);
                return p;
            }
            long amount = vnd(booking.getTotalAmount());
            String key = "cinema-stripe-" + UUID.randomUUID();
            var request = requests.save(PaymentGatewayRequest.builder().bookingId(bookingId).gateway("STRIPE")
                    .requestId(key).gatewayRequestId(key).orderId(key).amount(amount)
                    .status(PaymentGatewayRequestStatus.CREATED).build());
            StripePayment p = new StripePayment(bookingId, request.getPaymentRequestId(), mode, amount, "vnd",
                    config.getAccountId(), key, null, null, null, "CREATED", deadline, now(),
                    config.getSuccessUrl(), config.getCancelUrl(), scenario);
            store.insert(p);
            return p;
        }); // Reservation is committed BEFORE any provider request.
    }

    public StripePayment create(long userId, long bookingId, String mode, String scenario) {
        StripePayment reserved = reserve(userId, bookingId, mode, scenario);
        StripePayment p = ensureCreated(reserved);
        if ("API".equals(mode)) {
            try {
                gateway.verifyAccount();
                PaymentIntent intent = gateway.intent(p.intentId());
                verifyIntent(p, intent);
                if (p.deadline().isAfter(now()) && "requires_payment_method".equals(intent.getStatus())) gateway.confirm(p);
            } catch (com.stripe.exception.CardException e) {
                throw new BusinessException("Sandbox card declined; no ticket issued", ErrorCode.VALIDATION_FAILED);
            } catch (StripeException e) {
                // IDs remain durable. Never expose Stripe exception bodies/client secrets.
                throw unavailable();
            }
        }
        return p;
    }

    public StripePayment ensureCreated(StripePayment reserved) {
        return tx.execute(status -> {
            StripePayment p = store.lock(reserved.bookingId());
            if (p.sessionId() != null || p.intentId() != null) return p;
            // Stripe may prune idempotency keys after 24h. Never replay a creation beyond that horizon.
            if (!p.createdAt().plusHours(23).isAfter(now())) {
                store.state(p.bookingId(), "REVIEW_REQUIRED");
                return p;
            }
            try {
                if ("CHECKOUT".equals(p.mode())) {
                    Session s = gateway.createCheckout(p);
                    verifySession(p, s);
                    store.bind(p.bookingId(), s.getId(), s.getPaymentIntent(), s.getUrl());
                } else {
                    PaymentIntent i = gateway.createIntent(p);
                    verifyIntent(p, i);
                    store.bind(p.bookingId(), null, i.getId(), null);
                }
                return store.find(p.bookingId()).orElseThrow();
            } catch (StripeException e) { throw unavailable(); }
        });
    }

    public void verifySession(StripePayment p, Session s) {
        if (!Boolean.FALSE.equals(s.getLivemode()) || !"payment".equals(s.getMode()) ||
                !Long.valueOf(p.amount()).equals(s.getAmountTotal()) || !p.currency().equals(s.getCurrency()) ||
                !Long.toString(p.bookingId()).equals(s.getClientReferenceId()) ||
                (p.sessionId() != null && !p.sessionId().equals(s.getId())) ||
                (p.intentId() != null && !p.intentId().equals(s.getPaymentIntent())) ||
                !config.getAccountId().equals(p.accountId())) throw new IllegalStateException("Stripe session mismatch");
    }
    public void verifyIntent(StripePayment p, PaymentIntent i) {
        if (!Boolean.FALSE.equals(i.getLivemode()) || !Long.valueOf(p.amount()).equals(i.getAmount()) ||
                !p.currency().equals(i.getCurrency()) || (p.intentId() != null && !p.intentId().equals(i.getId())) ||
                !config.getAccountId().equals(p.accountId()) || i.getTransferData() != null || i.getOnBehalfOf() != null)
            throw new IllegalStateException("Stripe PaymentIntent mismatch");
        if ("succeeded".equals(i.getStatus()) && !Long.valueOf(p.amount()).equals(i.getAmountReceived()))
            throw new IllegalStateException("Stripe received amount mismatch");
    }
    private BusinessException unavailable() {
        return new BusinessException("Stripe outcome unavailable; retry same booking", ErrorCode.GATEWAY_TIMEOUT);
    }
}
