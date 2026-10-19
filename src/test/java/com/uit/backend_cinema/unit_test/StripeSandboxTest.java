package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.modules.booking.domain.entity.*;
import com.uit.backend_cinema.modules.booking.domain.repository.*;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.payment.domain.entity.*;
import com.uit.backend_cinema.modules.payment.domain.repository.*;
import com.uit.backend_cinema.modules.payment.domain.service.PaymentRequestService;
import com.uit.backend_cinema.modules.payment.stripe.api.StripeSandboxController;
import com.uit.backend_cinema.modules.payment.stripe.config.StripeSandboxConfig;
import com.uit.backend_cinema.modules.payment.stripe.domain.*;
import com.uit.backend_cinema.modules.payment.stripe.infrastructure.*;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StripeSandboxTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final LocalDateTime now = LocalDateTime.now(clock);
    private BookingRepository bookings;
    private PaymentRequestService requests;
    private StripePaymentStore store;
    private StripeGateway gateway;
    private StripeSandboxConfig config;
    private StripePaymentService payments;
    private StripeSettlementService settlement;
    private StripeWebhookService webhooks;
    private StripeRefundProcessor refundProcessor;
    private PaymentReceiptRepository receipts;
    private RefundRepository refunds;
    private TicketService tickets;
    private Booking booking;
    private StripePayment payment;
    private TransactionTemplate tx;

    @BeforeEach void setup() {
        bookings = mock(BookingRepository.class); requests = mock(PaymentRequestService.class);
        store = mock(StripePaymentStore.class); gateway = mock(StripeGateway.class);
        receipts = mock(PaymentReceiptRepository.class); refunds = mock(RefundRepository.class);
        tickets = mock(TicketService.class); refundProcessor = mock(StripeRefundProcessor.class);
        tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
        config = new StripeSandboxConfig(); config.setSecretKey("sk_test_fixture"); config.setWebhookSecret("whsec_fixture");
        config.setAccountId("acct_fixture"); config.setSuccessUrl("https://example.com/success"); config.setCancelUrl("https://example.com/cancel");
        payments = new StripePaymentService(bookings, requests, store, gateway, config, tx, clock);
        settlement = mock(StripeSettlementService.class);
        webhooks = new StripeWebhookService(config, store, gateway, payments, settlement, refundProcessor, tx, new ObjectMapper());
        payment = new StripePayment(1L, 2L, "API", 98000, "vnd", "acct_fixture", "cinema_test", null, "pi_test", null,
                "CREATED", now.plusMinutes(15), now, config.getSuccessUrl(), config.getCancelUrl(), "pm_card_visa");
        booking = new Booking(); booking.setBookingId(1L); booking.setUserId(17L); booking.setShowtimeId(3L);
        booking.setPaymentMethod(BookingPaymentMethod.STRIPE); booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(new BigDecimal("98000")); booking.setPaymentDeadline(now.plusMinutes(15));
        booking.setReconciliationDeadline(now.plusMinutes(15).plusSeconds(30));
    }
    private PaymentIntent intent() {
        PaymentIntent i = new PaymentIntent(); i.setId("pi_test"); i.setLivemode(false); i.setAmount(98000L);
        i.setAmountReceived(98000L); i.setCurrency("vnd"); i.setStatus("succeeded"); return i;
    }
    private StripeSettlementService realSettlement() {
        when(bookings.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(receipts.save(any())).thenAnswer(a -> { PaymentReceipt r=a.getArgument(0); r.setPaymentReceiptId(9L); return r; });
        when(requests.findById(2L)).thenReturn(Optional.of(PaymentGatewayRequest.builder().paymentRequestId(2L).build()));
        when(tickets.findActiveDraftSeatIds(1L)).thenReturn(List.of(12L));
        return new StripeSettlementService(bookings, receipts, refunds, mock(PaymentConfirmationRepository.class), requests,
                tickets, mock(SeatService.class), mock(FoodOrderService.class), mock(OutboxEventService.class), new ObjectMapper());
    }
    private String event(String type, String object) {
        return "{\"id\":\"evt_test\",\"object\":\"event\",\"type\":\""+type+"\",\"livemode\":false,\"created\":"+clock.instant().getEpochSecond()+",\"data\":{\"object\":{\"id\":\""+object+"\"}}}";
    }
    private String sign(String raw, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(config.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "t="+timestamp+",v1="+HexFormat.of().formatHex(mac.doFinal((timestamp+"."+raw).getBytes(StandardCharsets.UTF_8)));
    }
    private String sign(String raw) throws Exception { return sign(raw, Instant.now().getEpochSecond()); }

    @Test void vndIsNotMultipliedBy100() { assertEquals(98000, StripePaymentService.vnd(new BigDecimal("98000.00"))); }
    @ParameterizedTest @ValueSource(strings={"0", "-1", "0.5", "98000.01", "99999999999999999999999999"})
    void invalidMoneyRejected(String value) { assertThrows(RuntimeException.class, () -> StripePaymentService.vnd(new BigDecimal(value))); }
    @Test void configAcceptsSandboxOnly() { assertDoesNotThrow(config::validate); config.setSecretKey("sk_live_bad"); assertThrows(IllegalStateException.class, config::validate); }
    @Test void configRejectsFalseTestMode() { config.setTestMode(false); assertThrows(IllegalStateException.class, config::validate); }
    @Test void configRejectsMissingSigningSecret() { config.setWebhookSecret(null); assertThrows(IllegalStateException.class, config::validate); }
    @Test void driverRequiresStrongSeparateToken() {
        config.setSandboxDriverEnabled(true); assertThrows(IllegalStateException.class, config::validate);
        config.setSandboxToken("test-only-automation-token-32-characters"); assertDoesNotThrow(config::validate);
    }
    @Test void driverRejectsMissingTokenBeforePayment() {
        var driver = new StripeSandboxController(payments, config);
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> driver.pay(1, new StripeSandboxController.Scenario("SUCCESS"), null, null)).getStatusCode().value());
        verifyNoInteractions(gateway);
    }
    @Test void reserveRejectsAnotherOwner() {
        when(bookings.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        assertThrows(BusinessException.class, () -> payments.reserve(18, 1, "API", "pm_card_visa")); verifyNoInteractions(gateway, store);
    }
    @Test void reserveRejectsDeadlineBoundary() {
        booking.setPaymentDeadline(now); when(bookings.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        assertThrows(BusinessException.class, () -> payments.reserve(17, 1, "API", "pm_card_visa")); verifyNoInteractions(gateway);
    }
    @Test void reserveReusesOriginalRequest() {
        when(bookings.findByIdForUpdate(1L)).thenReturn(Optional.of(booking)); when(store.find(1L)).thenReturn(Optional.of(payment));
        assertSame(payment, payments.reserve(17, 1, "API", "pm_card_visa")); verify(requests, never()).save(any());
        assertThrows(BusinessException.class, () -> payments.reserve(17, 1, "CHECKOUT", null));
    }
    @Test void rejectsRawOrArbitraryPaymentMethods() { assertThrows(BusinessException.class, () -> payments.reserve(17, 1, "API", "4242424242424242")); }
    @Test void rejectsMismatchedProviderPayment() {
        var i = intent(); i.setAmount(1L); assertThrows(IllegalStateException.class, () -> payments.verifyIntent(payment,i));
        var live=intent(); live.setLivemode(true); assertThrows(IllegalStateException.class, () -> payments.verifyIntent(payment,live));
    }
    @Test void successfulSettlementIssuesTickets() {
        realSettlement().settle(payment,"pi_test",now,now);
        assertEquals(BookingStatus.PAID, booking.getStatus()); verify(tickets).finalizeTicketsForPaidBooking(1L,3L);
        verify(refunds,never()).save(any()); verify(receipts).save(any());
    }
    @Test void sameIntentDoesNotIssueTwice() {
        var s=realSettlement(); when(receipts.findByGatewayAndTransactionReference("STRIPE","pi_test"))
                .thenReturn(Optional.of(PaymentReceipt.builder().bookingId(1L).build()));
        s.settle(payment,"pi_test",now,now); verify(receipts,never()).save(any()); verify(tickets,never()).finalizeTicketsForPaidBooking(anyLong(),anyLong());
    }
    @Test void latePaymentCreatesFullRefundNotTickets() {
        realSettlement().settle(payment,"pi_test",now.plusMinutes(16),now.plusMinutes(16));
        verify(refunds).save(argThat(r -> r.getCause()==RefundCause.EXPIRED_LATE_PAYMENT && r.getAmount().intValueExact()==98000));
        verify(tickets,never()).finalizeTicketsForPaidBooking(anyLong(),anyLong());
    }
    @Test void signedOnTimePaymentWithinReconciliationWindowIsAccepted() {
        realSettlement().settle(payment,"pi_test",now.plusMinutes(14),now.plusMinutes(15).plusSeconds(10));
        assertEquals(BookingStatus.PAID,booking.getStatus());
    }
    @Test void adminCancelledBookingNeverResurrectsBeforePaymentDeadline() {
        booking.setStatus(BookingStatus.CANCELLED);
        realSettlement().settle(payment,"pi_test",now,now);
        assertEquals(BookingStatus.CANCELLED,booking.getStatus());
        verify(refunds).save(argThat(r -> r.getAmount().intValueExact()==98000));
        verify(tickets,never()).finalizeTicketsForPaidBooking(anyLong(),anyLong());
    }
    @Test void expiredBookingNeverResurrects() {
        booking.setStatus(BookingStatus.EXPIRED); realSettlement().settle(payment,"pi_test",now,now);
        assertEquals(BookingStatus.EXPIRED,booking.getStatus()); verify(refunds).save(any());
    }
    @Test void secondChargeIsCompensated() {
        booking.setStatus(BookingStatus.PAID); realSettlement().settle(payment,"pi_second",now,now);
        verify(refunds).save(argThat(r -> r.getCause()==RefundCause.DUPLICATE_CHARGE));
    }
    @Test void invalidSignatureNeverTouchesStorage() {
        assertThrows(ResponseStatusException.class, () -> webhooks.handle(event("payment_intent.succeeded","pi_test"),"bad"));
        verifyNoInteractions(store,gateway);
    }
    @Test void validSignatureAndReplayTolerance() throws Exception {
        String raw=event("payment_intent.succeeded","pi_test"); assertEquals("evt_test",webhooks.verify(raw,sign(raw)).getId());
        assertThrows(ResponseStatusException.class, () -> webhooks.verify(raw,sign(raw, Instant.now().minusSeconds(600).getEpochSecond())));
        assertThrows(ResponseStatusException.class, () -> webhooks.verify(raw.replace("false","true"),sign(raw.replace("false","true"))));
    }
    @Test void webhookRetryDoesNotProcessAgain() throws Exception {
        String raw=event("payment_intent.succeeded","pi_test"); when(store.receive("evt_test","payment_intent.succeeded")).thenReturn(false);
        webhooks.handle(raw,sign(raw)); verifyNoInteractions(gateway,settlement);
    }
    @Test void webhookRetrievesProviderStateBeforeSettlement() throws Exception {
        String raw=event("payment_intent.succeeded","pi_test"); when(store.receive(anyString(),anyString())).thenReturn(true);
        when(store.byObject("pi_test")).thenReturn(Optional.of(payment)); when(store.lock(1L)).thenReturn(payment);
        when(gateway.intent("pi_test")).thenReturn(intent()); webhooks.handle(raw,sign(raw));
        verify(settlement).settle(eq(payment),eq("pi_test"),eq(now),eq(now)); verify(store).processed("evt_test");
    }
    @Test void failureEventCannotRegressSuccessfulProviderPayment() throws Exception {
        String raw=event("payment_intent.payment_failed","pi_test"); when(store.receive(anyString(),anyString())).thenReturn(true);
        when(store.byObject("pi_test")).thenReturn(Optional.of(payment)); when(store.lock(1L)).thenReturn(payment);
        when(gateway.intent("pi_test")).thenReturn(intent()); webhooks.handle(raw,sign(raw));
        verify(store).state(1L,"SUCCEEDED");
    }
    @Test void paymentFailureDoesNotSettle() throws Exception {
        String raw=event("payment_intent.payment_failed","pi_test"); when(store.receive(anyString(),anyString())).thenReturn(true);
        when(store.byObject("pi_test")).thenReturn(Optional.of(payment)); when(store.lock(1L)).thenReturn(payment);
        var i=intent(); i.setStatus("requires_payment_method"); when(gateway.intent("pi_test")).thenReturn(i);
        webhooks.handle(raw,sign(raw)); verifyNoInteractions(settlement); verify(store).processed("evt_test");
    }
    @Test void checkoutIntentAssociationUsesAuthenticatedSession() throws Exception {
        String raw=event("payment_intent.succeeded","pi_test"); when(store.receive(anyString(),anyString())).thenReturn(true);
        StripePayment checkout = new StripePayment(1,2,"CHECKOUT",98000,"vnd","acct_fixture","cinema_test","cs_test",null,"https://checkout.stripe.com/test","CREATED",now.plusMinutes(15),now,config.getSuccessUrl(),config.getCancelUrl(),null);
        Session s=new Session(); s.setId("cs_test"); s.setPaymentIntent("pi_test"); s.setLivemode(false); s.setAmountTotal(98000L); s.setCurrency("vnd"); s.setMode("payment"); s.setClientReferenceId("1");
        when(gateway.sessionsForIntent("pi_test")).thenReturn(List.of(s)); when(store.byObject("cs_test")).thenReturn(Optional.of(checkout));
        when(store.lock(1L)).thenReturn(checkout); when(store.find(1L)).thenReturn(Optional.of(payment));
        when(gateway.session("cs_test")).thenReturn(s); when(gateway.intent("pi_test")).thenReturn(intent());
        webhooks.handle(raw,sign(raw)); verify(store,atLeastOnce()).bind(1L,"cs_test","pi_test",null); verify(settlement).settle(eq(payment),eq("pi_test"),eq(now),eq(now));
    }
    @ParameterizedTest @CsvSource({"succeeded,SUCCEEDED","pending,PROCESSING","failed,FAILED","canceled,FAILED","requires_action,REVIEW_REQUIRED","unknown,UNKNOWN"})
    void refundStatusRequiresEvidence(String provider, RefundStatus expected) { assertEquals(expected,StripeRefundProcessor.mapStatus(provider)); }
    @Test void refundWebhookIsProcessedNotSilentlyIgnored() throws Exception {
        String raw=event("refund.updated","re_test"); when(store.receive(anyString(),anyString())).thenReturn(true);
        webhooks.handle(raw,sign(raw)); verify(refundProcessor).handleEvent("re_test"); verify(store).processed("evt_test");
    }
}
