package com.uit.backend_cinema.modules.payment.stripe.infrastructure;

import com.uit.backend_cinema.modules.payment.stripe.domain.StripePayment;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripePaymentStore {
    private final JdbcTemplate jdbc;
    private final RowMapper<StripePayment> mapper = (rs, n) -> new StripePayment(
            rs.getLong("booking_id"), rs.getLong("payment_request_id"), rs.getString("mode"),
            rs.getLong("amount"), rs.getString("currency"), rs.getString("account_id"),
            rs.getString("idempotency_key"), rs.getString("session_id"), rs.getString("intent_id"),
            rs.getString("pay_url"), rs.getString("state"), rs.getTimestamp("deadline").toLocalDateTime(),
            rs.getTimestamp("created_at").toLocalDateTime(), rs.getString("success_url"),
            rs.getString("cancel_url"), rs.getString("scenario"));

    public Optional<StripePayment> find(long bookingId) {
        return jdbc.query("SELECT * FROM stripe_payments WHERE booking_id=?", mapper, bookingId).stream().findFirst();
    }
    public StripePayment lock(long bookingId) {
        return jdbc.queryForObject("SELECT * FROM stripe_payments WHERE booking_id=? FOR UPDATE", mapper, bookingId);
    }
    public Optional<StripePayment> byObject(String id) {
        return jdbc.query("SELECT * FROM stripe_payments WHERE session_id=? OR intent_id=?", mapper, id, id).stream().findFirst();
    }
    public void insert(StripePayment p) {
        jdbc.update("""
                INSERT INTO stripe_payments (booking_id,payment_request_id,mode,amount,currency,account_id,
                idempotency_key,deadline,created_at,success_url,cancel_url,scenario) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, p.bookingId(), p.requestId(), p.mode(), p.amount(), p.currency(), p.accountId(),
                p.idempotencyKey(), p.deadline(), p.createdAt(), p.successUrl(), p.cancelUrl(), p.scenario());
    }
    public void bind(long id, String session, String intent, String url) {
        jdbc.update("UPDATE stripe_payments SET session_id=COALESCE(session_id,?), intent_id=COALESCE(intent_id,?), pay_url=COALESCE(pay_url,?) WHERE booking_id=?",
                session, intent, url, id);
    }
    public void state(long id, String state) {
        jdbc.update("UPDATE stripe_payments SET state=? WHERE booking_id=? AND state<>'SUCCEEDED'", state, id);
    }
    public boolean receive(String eventId, String type) {
        return jdbc.update("INSERT INTO stripe_webhook_inbox(event_id,event_type) VALUES (?,?) ON CONFLICT DO NOTHING", eventId, type) == 1;
    }
    public void processed(String eventId) {
        jdbc.update("UPDATE stripe_webhook_inbox SET processed_at=CURRENT_TIMESTAMP WHERE event_id=?", eventId);
    }
    public List<StripePayment> unfinished() {
        return jdbc.query("SELECT * FROM stripe_payments WHERE state NOT IN ('SUCCEEDED','EXPIRED','REVIEW_REQUIRED') ORDER BY created_at LIMIT 100", mapper);
    }
}
