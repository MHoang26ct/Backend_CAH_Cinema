package com.uit.backend_cinema.modules.payment.stripe.domain;

import com.uit.backend_cinema.modules.payment.domain.entity.*;
import com.uit.backend_cinema.modules.payment.domain.repository.*;
import com.uit.backend_cinema.modules.payment.domain.service.RefundMembershipService;
import com.uit.backend_cinema.modules.payment.stripe.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeRefundProcessor {
    private final RefundRepository refunds;
    private final PaymentReceiptRepository receipts;
    private final StripePaymentStore store;
    private final StripeGateway gateway;
    private final StripePaymentService payments;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RefundMembershipService refundMembershipService;

    public void process(long id) {
        Boolean send = tx.execute(status -> {
            Refund r = refunds.findById(id).orElseThrow();
            // Serialize all obligations for this receipt, not just a single refund.
            jdbc.queryForObject("SELECT payment_receipt_id FROM payment_receipts WHERE payment_receipt_id=? FOR UPDATE", Long.class, r.getPaymentReceiptId());
            r = refunds.findById(id).orElseThrow();
            if (r.getStatus() != RefundStatus.REQUESTED) return false;
            PaymentReceipt receipt = receipts.findById(r.getPaymentReceiptId()).orElseThrow();
            StripePayment p = store.find(r.getBookingId()).orElseThrow();
            BigDecimal reserved = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM refunds WHERE payment_receipt_id=? AND refund_id<>? AND status<>'FAILED'",
                    BigDecimal.class, r.getPaymentReceiptId(), id);
            if (!validOwnership(r, receipt, p) || r.getAmount().signum() <= 0 ||
                    r.getAmount().add(reserved).compareTo(receipt.getAmount()) > 0) {
                r.setStatus(RefundStatus.REVIEW_REQUIRED);
                refunds.save(r);
                return false;
            }
            StripePaymentService.vnd(r.getAmount());
            r.setStatus(RefundStatus.PROCESSING);
            r.setClaimedAt(payments.now());
            refunds.save(r);
            return true;
        }); // Durable claim before external request; a crash is reconciled, never blindly replayed.
        Refund r = refunds.findById(id).orElseThrow();
        if (!List.of(RefundStatus.PROCESSING, RefundStatus.UNKNOWN).contains(r.getStatus())) return;
        try {
            PaymentReceipt receipt = receipts.findById(r.getPaymentReceiptId()).orElseThrow();
            StripePayment p = store.find(r.getBookingId()).orElseThrow();
            if (!validOwnership(r, receipt, p)) throw new IllegalStateException();
            gateway.verifyAccount();
            var intent = gateway.intent(p.intentId());
            payments.verifyIntent(p, intent);
            if (!"succeeded".equals(intent.getStatus())) throw new IllegalStateException();
            String key = "cinema-stripe-refund-" + r.getRefundId();
            com.stripe.model.Refund result = null;
            if (r.getGatewayTransactionNo() != null) result = gateway.refundById(r.getGatewayTransactionNo());
            else {
                // Always reconcile first, including the first submission. Metadata is only a selector;
                // verify intent, currency and amount against our durable receipt below.
                for (var candidate : gateway.refunds(p.intentId())) {
                    if (candidate.getMetadata() != null && key.equals(candidate.getMetadata().get("refund_reference"))) {
                        if (result != null) throw new IllegalStateException("Multiple matching refunds");
                        result = candidate;
                    }
                }
            }
            if (result == null && (Boolean.TRUE.equals(send) || (r.getClaimedAt() != null &&
                    r.getClaimedAt().plusSeconds(30).isBefore(payments.now()) && r.getClaimedAt().plusHours(23).isAfter(payments.now())))) {
                result = gateway.refund(p.intentId(), StripePaymentService.vnd(r.getAmount()), key);
            }
            if (result == null && r.getClaimedAt() != null && !r.getClaimedAt().plusHours(23).isAfter(payments.now())) {
                update(id, null, RefundStatus.REVIEW_REQUIRED);
                return;
            }
            if (result == null) { update(id, null, RefundStatus.UNKNOWN); return; }
            if (!p.intentId().equals(result.getPaymentIntent()) || !p.currency().equals(result.getCurrency()) ||
                    !Long.valueOf(StripePaymentService.vnd(r.getAmount())).equals(result.getAmount())) throw new IllegalStateException();
            update(id, result, mapStatus(result.getStatus()));
        } catch (Exception e) {
            update(id, null, RefundStatus.UNKNOWN);
        }
    }
    public void handleEvent(String providerRefundId) throws Exception {
        gateway.verifyAccount();
        var result = gateway.refundById(providerRefundId);
        String reference = result.getMetadata() == null ? null : result.getMetadata().get("refund_reference");
        if (reference == null || !reference.matches("cinema-stripe-refund-[0-9]+")) return;
        long id = Long.parseLong(reference.substring("cinema-stripe-refund-".length()));
        Refund r = refunds.findById(id).orElseThrow();
        PaymentReceipt receipt = receipts.findById(r.getPaymentReceiptId()).orElseThrow();
        StripePayment p = store.find(r.getBookingId()).orElseThrow();
        if (!validOwnership(r, receipt, p) || !p.intentId().equals(result.getPaymentIntent()) ||
                !p.currency().equals(result.getCurrency()) ||
                !Long.valueOf(StripePaymentService.vnd(r.getAmount())).equals(result.getAmount()) ||
                (r.getGatewayTransactionNo() != null && !r.getGatewayTransactionNo().equals(result.getId()))) {
            throw new IllegalStateException("Stripe refund mismatch");
        }
        update(id, result, mapStatus(result.getStatus()));
    }

    private boolean validOwnership(Refund r, PaymentReceipt receipt, StripePayment p) {
        return "STRIPE".equals(receipt.getGateway()) && receipt.getBookingId().equals(r.getBookingId()) &&
                receipt.getPaymentRequestId() != null && receipt.getPaymentRequestId() == p.requestId() &&
                p.intentId() != null && p.intentId().equals(receipt.getTransactionReference()) &&
                BigDecimal.valueOf(p.amount()).compareTo(receipt.getAmount()) == 0;
    }
    public static RefundStatus mapStatus(String status) {
        if (status == null) return RefundStatus.UNKNOWN;
        return switch (status) {
            case "succeeded" -> RefundStatus.SUCCEEDED; // Authenticated API object evidence, not HTTP 200.
            case "pending" -> RefundStatus.PROCESSING;
            case "failed", "canceled" -> RefundStatus.FAILED;
            case "requires_action" -> RefundStatus.REVIEW_REQUIRED;
            default -> RefundStatus.UNKNOWN;
        };
    }
    private void update(long id, com.stripe.model.Refund result, RefundStatus state) {
        tx.executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT refund_id FROM refunds WHERE refund_id=? FOR UPDATE", Long.class, id);
            Refund current = refunds.findById(id).orElseThrow();
            if (current.getStatus() == RefundStatus.FAILED) return;
            // Stripe can report a bank-side failure after initially succeeding. Preserve that evidence.
            if (current.getStatus() == RefundStatus.SUCCEEDED && (result == null || state != RefundStatus.FAILED)) return;
            current.setStatus(state);
            if (result != null) {
                current.setGatewayTransactionNo(result.getId());
                current.setGatewayResponseCode(result.getStatus());
                if (state == RefundStatus.SUCCEEDED || state == RefundStatus.FAILED) current.setProcessedAt(payments.now());
            }
            refunds.save(current);
        });
        if (refundMembershipService != null) refundMembershipService.reconcile(id);
    }
}
