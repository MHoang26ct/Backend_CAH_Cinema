package com.uit.backend_cinema.modules.payment.stripe.domain;

import com.uit.backend_cinema.modules.payment.stripe.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeExpiryWorker {
    private static final Logger log = LoggerFactory.getLogger(StripeExpiryWorker.class);
    private final StripePaymentStore store;
    private final StripePaymentService payments;
    private final StripeGateway gateway;
    private final StripeWebhookService webhooks;

    @Scheduled(fixedDelayString = "${stripe.expiry-interval-ms:5000}")
    public void expire() {
        for (StripePayment candidate : store.unfinished()) {
            try {
                if (candidate.deadline().isAfter(payments.now())) {
                    if (candidate.createdAt().plusMinutes(2).isBefore(payments.now())) webhooks.reconcile(candidate);
                    continue;
                }
                // Recover an interrupted creation using the persisted key, then close it.
                StripePayment p = payments.ensureCreated(candidate);
                gateway.verifyAccount();
                if (p.sessionId() != null) {
                    var session = gateway.session(p.sessionId());
                    payments.verifySession(p, session);
                    if ("open".equals(session.getStatus())) gateway.expire(p.sessionId());
                    if (!"complete".equals(session.getStatus())) store.state(p.bookingId(), "EXPIRED");
                } else if (p.intentId() != null) {
                    var intent = gateway.intent(p.intentId());
                    payments.verifyIntent(p, intent);
                    if (!"succeeded".equals(intent.getStatus()) && !"canceled".equals(intent.getStatus())) gateway.cancel(p.intentId());
                    if (!"succeeded".equals(intent.getStatus())) store.state(p.bookingId(), "EXPIRED");
                }
                // Authenticated retrieval covers a lost webhook and records late-payment compensation.
                webhooks.reconcile(p);
            } catch (Exception e) {
                log.warn("Stripe expiration deferred for booking {}", candidate.bookingId());
            }
        }
    }
}
