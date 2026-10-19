package com.uit.backend_cinema.modules.payment.stripe.infrastructure;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.uit.backend_cinema.modules.payment.stripe.config.StripeSandboxConfig;
import com.uit.backend_cinema.modules.payment.stripe.domain.StripePayment;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeGateway {
    private final StripeClient client;
    private final StripeSandboxConfig config;

    public void verifyAccount() throws StripeException {
        if (!config.getAccountId().equals(client.v1().accounts().retrieveCurrent().getId())) {
            throw new IllegalStateException("Stripe account mismatch");
        }
    }
    private RequestOptions options(String key) {
        return RequestOptions.builder().setIdempotencyKey(key).build();
    }
    public Session createCheckout(StripePayment p) throws StripeException {
        verifyAccount();
        var price = SessionCreateParams.LineItem.PriceData.builder().setCurrency(p.currency()).setUnitAmount(p.amount())
                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName("Cinema booking " + p.bookingId()).build()).build();
        // Stripe's minimum expires_at is 30 minutes, not our 15-minute business hold.
        // Default provider TTL is 24h; the deadline worker explicitly expires this session.
        return client.v1().checkout().sessions().create(SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT).addAllowedPaymentMethodType(SessionCreateParams.AllowedPaymentMethodType.CARD)
                .setAdaptivePricing(SessionCreateParams.AdaptivePricing.builder().setEnabled(false).build())
                .setSuccessUrl(p.successUrl()).setCancelUrl(p.cancelUrl()).setClientReferenceId(Long.toString(p.bookingId()))
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder().putMetadata("cinema_request", p.idempotencyKey()).build())
                .addLineItem(SessionCreateParams.LineItem.builder().setPriceData(price).setQuantity(1L).build())
                .build(), options(p.idempotencyKey()));
    }
    public PaymentIntent createIntent(StripePayment p) throws StripeException {
        verifyAccount();
        // Persist ID before confirmation so webhook association never depends on metadata.
        return client.v1().paymentIntents().create(PaymentIntentCreateParams.builder().setAmount(p.amount())
                .setCurrency(p.currency()).addAllowedPaymentMethodType(PaymentIntentCreateParams.AllowedPaymentMethodType.CARD)
                .putMetadata("cinema_request", p.idempotencyKey()).build(), options(p.idempotencyKey()));
    }
    public PaymentIntent confirm(StripePayment p) throws StripeException {
        return client.v1().paymentIntents().confirm(p.intentId(), PaymentIntentConfirmParams.builder()
                .setPaymentMethod(p.scenario()).build(), options(p.idempotencyKey() + ":confirm"));
    }
    public Iterable<Session> sessionsForIntent(String id) throws StripeException {
        return client.v1().checkout().sessions().list(com.stripe.param.checkout.SessionListParams.builder()
                .setPaymentIntent(id).setLimit(100L).build()).autoPagingIterable();
    }
    public Session session(String id) throws StripeException { return client.v1().checkout().sessions().retrieve(id); }
    public PaymentIntent intent(String id) throws StripeException { return client.v1().paymentIntents().retrieve(id); }
    public void expire(String id) throws StripeException { client.v1().checkout().sessions().expire(id); }
    public void cancel(String id) throws StripeException { client.v1().paymentIntents().cancel(id); }
    public Refund refund(String intent, long amount, String key) throws StripeException {
        return client.v1().refunds().create(RefundCreateParams.builder().setPaymentIntent(intent).setAmount(amount)
                .putMetadata("refund_reference", key).build(), options(key));
    }
    public Refund refundById(String id) throws StripeException { return client.v1().refunds().retrieve(id); }
    public Iterable<Refund> refunds(String intent) throws StripeException {
        return client.v1().refunds().list(RefundListParams.builder().setPaymentIntent(intent).setLimit(100L).build()).autoPagingIterable();
    }
}
