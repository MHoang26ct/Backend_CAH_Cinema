package com.uit.backend_cinema.modules.payment.stripe.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.uit.backend_cinema.modules.payment.stripe.config.StripeSandboxConfig;
import com.uit.backend_cinema.modules.payment.stripe.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeWebhookService {
    private static final Set<String> TYPES = Set.of("checkout.session.completed", "checkout.session.async_payment_succeeded",
            "checkout.session.expired", "payment_intent.succeeded", "payment_intent.payment_failed", "payment_intent.canceled");
    private static final Set<String> REFUND_TYPES = Set.of("refund.created", "refund.updated", "refund.failed");
    private final StripeSandboxConfig config;
    private final StripePaymentStore store;
    private final StripeGateway gateway;
    private final StripePaymentService payments;
    private final StripeSettlementService settlement;
    private final StripeRefundProcessor refunds;
    private final TransactionTemplate tx;
    private final ObjectMapper json;

    public Event verify(String raw, String signature) {
        try {
            Event event = Webhook.constructEvent(raw, signature, config.getWebhookSecret(), 300);
            if (!Boolean.FALSE.equals(event.getLivemode()) || event.getAccount() != null ||
                    json.readTree(raw).hasNonNull("context") || event.getId() == null || event.getType() == null)
                throw new IllegalArgumentException();
            return event;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe event/signature");
        }
    }

    public void handle(String raw, String signature) {
        Event event = verify(raw, signature);
        tx.executeWithoutResult(status -> {
            if (!store.receive(event.getId(), event.getType())) return;
            try {
                String objectId = json.readTree(raw).path("data").path("object").path("id").asText();
                if (TYPES.contains(event.getType())) {
                    gateway.verifyAccount();
                    StripePayment p = resolvePayment(objectId, event.getType());
                    if (p != null) reconcileLocked(p, "payment_intent.succeeded".equals(event.getType())
                            ? java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(event.getCreated()), java.time.ZoneId.of("Asia/Ho_Chi_Minh")) : payments.now());
                } else if (REFUND_TYPES.contains(event.getType())) {
                    refunds.handleEvent(objectId);
                }
                store.processed(event.getId());
            } catch (Exception e) {
                // No raw event, card details, Stripe exception body or secrets in application logs.
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe event processing deferred");
            }
        });
    }

    private StripePayment resolvePayment(String objectId, String type) throws Exception {
        var known = store.byObject(objectId);
        if (known.isPresent()) return known.get();
        if (type.startsWith("payment_intent.")) {
            // Checkout creates the PaymentIntent later; the original create response has no PI ID.
            // Bind only using an authenticated Session whose ID we already persisted, not event metadata.
            for (Session session : gateway.sessionsForIntent(objectId)) {
                var match = store.byObject(session.getId());
                if (match.isPresent()) {
                    StripePayment p = store.lock(match.get().bookingId());
                    payments.verifySession(p, session);
                    if (!objectId.equals(session.getPaymentIntent())) throw new IllegalStateException("Session intent mismatch");
                    store.bind(p.bookingId(), session.getId(), objectId, null);
                    return store.find(p.bookingId()).orElseThrow();
                }
            }
            PaymentIntent intent = gateway.intent(objectId);
            if (intent.getMetadata() != null && intent.getMetadata().containsKey("cinema_request"))
                throw new IllegalStateException("Cinema payment not yet bound; retry delivery");
        }
        return null; // Unrelated sandbox events/Stripe CLI fixtures do not belong to Cinema.
    }

    /** Authenticated recovery when webhook delivery is lost. Same locked settlement path. */
    public void reconcile(StripePayment candidate) {
        tx.executeWithoutResult(status -> {
            try {
                gateway.verifyAccount();
                reconcileLocked(candidate, payments.now());
            } catch (Exception e) {
                throw new IllegalStateException("Stripe reconciliation deferred");
            }
        });
    }

    private void reconcileLocked(StripePayment candidate, java.time.LocalDateTime paidAt) throws Exception {
        StripePayment p = store.lock(candidate.bookingId());
        PaymentIntent intent = null;
        if (p.sessionId() != null) {
            Session session = gateway.session(p.sessionId());
            payments.verifySession(p, session);
            if (session.getPaymentIntent() != null) {
                store.bind(p.bookingId(), session.getId(), session.getPaymentIntent(), null);
                p = store.find(p.bookingId()).orElseThrow();
                intent = gateway.intent(session.getPaymentIntent());
            } else if ("expired".equals(session.getStatus())) store.state(p.bookingId(), "EXPIRED");
        } else if (p.intentId() != null) intent = gateway.intent(p.intentId());
        if (intent != null) {
            payments.verifyIntent(p, intent);
            if ("succeeded".equals(intent.getStatus())) {
                settlement.settle(p, intent.getId(), paidAt, payments.now());
                store.state(p.bookingId(), "SUCCEEDED");
            } else if ("canceled".equals(intent.getStatus())) store.state(p.bookingId(), "EXPIRED");
        }
    }
}
