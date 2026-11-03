package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.PaymentIntent;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceipt;
import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import com.uit.backend_cinema.modules.payment.domain.service.PaymentRequestService;
import com.uit.backend_cinema.modules.payment.domain.service.RefundService;
import com.uit.backend_cinema.modules.payment.stripe.domain.StripePayment;
import com.uit.backend_cinema.modules.payment.stripe.domain.StripePaymentService;
import com.uit.backend_cinema.modules.payment.stripe.domain.StripeRefundProcessor;
import com.uit.backend_cinema.modules.payment.stripe.infrastructure.StripeGateway;
import com.uit.backend_cinema.modules.payment.stripe.infrastructure.StripePaymentStore;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.infrastructure.client.VnpayMerchantClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Business-level tests only: repositories, transaction manager and provider boundaries are mocked. */
class ScopeRefundDesignTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 17, 10, 0);
    private static final String RESERVED_SQL = "SELECT COALESCE(SUM(amount),0) FROM refunds WHERE payment_receipt_id=? AND refund_id<>? AND status<>'FAILED'";

    @ParameterizedTest(name = "UT13: requested={0}, reserved={1}, allowed={2}")
    @CsvSource({
            "100001, 0, false",
            "50001, 50000, false",
            "50000, 50000, true",
            "49999, 50000, true"
    })
    void ut13StripeOverRefundGuardIncludesExistingReservations(long amount, long reserved, boolean allowed) throws Exception {
        RefundRepository refunds = mock(RefundRepository.class);
        PaymentReceiptRepository receipts = mock(PaymentReceiptRepository.class);
        StripePaymentStore store = mock(StripePaymentStore.class);
        StripeGateway gateway = mock(StripeGateway.class);
        StripePaymentService payments = mock(StripePaymentService.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        StripeRefundProcessor processor = new StripeRefundProcessor(refunds, receipts, store, gateway, payments,
                jdbc, new TransactionTemplate(mock(PlatformTransactionManager.class)));
        Refund refund = Refund.builder().refundId(3L).paymentReceiptId(2L).bookingId(1L)
                .amount(BigDecimal.valueOf(amount)).status(RefundStatus.REQUESTED).build();
        PaymentReceipt receipt = PaymentReceipt.builder().paymentReceiptId(2L).bookingId(1L)
                .paymentRequestId(4L).gateway("STRIPE").transactionReference("pi_scope")
                .amount(new BigDecimal("100000")).build();
        StripePayment payment = new StripePayment(1, 4, "API", 100000, "vnd", "acct_scope", "key",
                null, "pi_scope", null, "SUCCEEDED", NOW, NOW, null, null, "pm_card_visa");
        when(refunds.findById(3L)).thenReturn(Optional.of(refund));
        when(receipts.findById(2L)).thenReturn(Optional.of(receipt));
        when(store.find(1L)).thenReturn(Optional.of(payment));
        when(jdbc.queryForObject(RESERVED_SQL, BigDecimal.class, 2L, 3L)).thenReturn(BigDecimal.valueOf(reserved));
        List<RefundStatus> persistedStates = new ArrayList<>();
        when(refunds.save(any())).thenAnswer(invocation -> {
            Refund saved = invocation.getArgument(0);
            persistedStates.add(saved.getStatus());
            return saved;
        });
        if (allowed) {
            when(payments.now()).thenReturn(NOW);
            PaymentIntent intent = new PaymentIntent();
            intent.setStatus("succeeded");
            when(gateway.intent("pi_scope")).thenReturn(intent);
            when(gateway.refunds("pi_scope")).thenReturn(List.of());
            com.stripe.model.Refund result = new com.stripe.model.Refund();
            result.setId("re_scope"); result.setPaymentIntent("pi_scope");
            result.setAmount(amount); result.setCurrency("vnd"); result.setStatus("succeeded");
            when(gateway.refund("pi_scope", amount, "cinema-stripe-refund-3")).thenReturn(result);
        }

        processor.process(3L);

        verify(jdbc).queryForObject("SELECT payment_receipt_id FROM payment_receipts WHERE payment_receipt_id=? FOR UPDATE",
                Long.class, 2L);
        verify(jdbc).queryForObject(RESERVED_SQL, BigDecimal.class, 2L, 3L);
        assertEquals(BigDecimal.valueOf(amount), refund.getAmount());
        assertEquals(new BigDecimal("100000"), receipt.getAmount());
        if (allowed) {
            assertEquals(RefundStatus.SUCCEEDED, refund.getStatus());
            assertEquals(List.of(RefundStatus.PROCESSING, RefundStatus.SUCCEEDED), persistedStates);
            assertEquals(NOW, refund.getClaimedAt());
            assertEquals(NOW, refund.getProcessedAt());
            verify(gateway).refund("pi_scope", amount, "cinema-stripe-refund-3");
        } else {
            assertEquals(RefundStatus.REVIEW_REQUIRED, refund.getStatus());
            assertEquals(List.of(RefundStatus.REVIEW_REQUIRED), persistedStates);
            assertNull(refund.getClaimedAt());
            assertNull(refund.getProcessedAt());
            verifyNoInteractions(gateway);
        }
    }

    @ParameterizedTest(name = "UT14: VNPay response {0}/{1} becomes {2}")
    @CsvSource({
            "00, 09, FAILED",
            "02, 00, FAILED",
            "03, 00, FAILED",
            "91, 00, FAILED",
            "95, 00, FAILED",
            "97, 00, FAILED",
            "99, 00, UNKNOWN",
            "00, 42, UNKNOWN",
            "00, , UNKNOWN"
    })
    void ut14VnpayDistinguishesDefinitiveFailureFromUnknownWithoutReplay(
            String responseCode, String transactionStatus, RefundStatus expected) {
        VnpayFixture fixture = new VnpayFixture();
        when(fixture.dispatch()).thenReturn(VnpayRefundResponseDTO.builder()
                .responseCode(responseCode).transactionStatus(transactionStatus)
                .transactionNo("refund_evidence").message("provider evidence").build());

        fixture.service.processOnlineRefund(10L);

        assertEquals(expected, fixture.refund.getStatus());
        assertEquals(List.of(RefundStatus.PROCESSING, expected), fixture.persistedStates);
        assertEquals(NOW, fixture.refund.getClaimedAt());
        assertEquals(responseCode, fixture.refund.getGatewayResponseCode());
        assertEquals("refund_evidence", fixture.refund.getGatewayTransactionNo());
        assertEquals(new BigDecimal("50000"), fixture.refund.getAmount());
        if (expected == RefundStatus.FAILED) {
            assertEquals(NOW, fixture.refund.getProcessedAt());
            assertEquals("provider evidence", fixture.refund.getGatewayResponseMessage());
        } else {
            assertNull(fixture.refund.getProcessedAt());
            assertTrue(fixture.refund.getGatewayResponseMessage().contains("no automatic retry"));
        }

        fixture.service.processOnlineRefund(10L);
        assertEquals(expected, fixture.refund.getStatus());
        assertEquals(List.of(RefundStatus.PROCESSING, expected), fixture.persistedStates);
        fixture.verifySingleDispatch();
    }

    @ParameterizedTest(name = "UT14: transport {0} stays UNKNOWN, then requires review")
    @EnumSource(value = ErrorCode.class, names = {"GATEWAY_TIMEOUT", "GATEWAY_COMMUNICATION_FAILED"})
    void ut14UnknownTransportFailureEscalatesAtFifteenMinutesWithoutResend(ErrorCode code) {
        VnpayFixture fixture = new VnpayFixture();
        when(fixture.dispatch()).thenThrow(new BusinessException("response unavailable", code));

        fixture.service.processOnlineRefund(10L);

        assertEquals(RefundStatus.UNKNOWN, fixture.refund.getStatus());
        assertEquals(List.of(RefundStatus.PROCESSING, RefundStatus.UNKNOWN), fixture.persistedStates);
        assertNull(fixture.refund.getProcessedAt());
        assertTrue(fixture.refund.getGatewayResponseMessage().contains(code.name()));
        fixture.time(NOW.plusSeconds(899));
        fixture.service.processOnlineRefund(10L);
        assertEquals(RefundStatus.UNKNOWN, fixture.refund.getStatus());
        assertEquals(2, fixture.persistedStates.size());

        fixture.time(NOW.plusSeconds(900));
        fixture.service.processOnlineRefund(10L);

        assertEquals(RefundStatus.REVIEW_REQUIRED, fixture.refund.getStatus());
        assertEquals(List.of(RefundStatus.PROCESSING, RefundStatus.UNKNOWN, RefundStatus.REVIEW_REQUIRED),
                fixture.persistedStates);
        assertEquals(NOW, fixture.refund.getClaimedAt());
        assertNull(fixture.refund.getProcessedAt());
        assertTrue(fixture.refund.getGatewayResponseMessage().contains("do not resend"));
        assertTrue(fixture.refund.getGatewayResponseMessage().contains(code.name()));
        fixture.verifySingleDispatch();
    }

    private static class VnpayFixture {
        final RefundRepository refunds = mock(RefundRepository.class);
        final PaymentReceiptRepository receipts = mock(PaymentReceiptRepository.class);
        final PaymentRequestService requests = mock(PaymentRequestService.class);
        final VnpayMerchantClient gateway = mock(VnpayMerchantClient.class);
        final Clock clock = mock(Clock.class);
        final List<RefundStatus> persistedStates = new ArrayList<>();
        final Refund refund = Refund.builder().refundId(10L).paymentReceiptId(20L).bookingId(5L)
                .amount(new BigDecimal("50000")).status(RefundStatus.REQUESTED).createdAt(NOW).build();
        final RefundService service;

        VnpayFixture() {
            when(clock.getZone()).thenReturn(ZoneOffset.UTC);
            time(NOW);
            when(refunds.findById(10L)).thenReturn(Optional.of(refund));
            when(refunds.findByIdForUpdate(10L)).thenReturn(Optional.of(refund));
            when(refunds.save(any())).thenAnswer(invocation -> {
                Refund saved = invocation.getArgument(0);
                persistedStates.add(saved.getStatus());
                return saved;
            });
            when(receipts.findById(20L)).thenReturn(Optional.of(PaymentReceipt.builder()
                    .paymentReceiptId(20L).paymentRequestId(30L).bookingId(5L).gateway("VNPAY")
                    .orderId("order_scope").transactionReference("payment_scope")
                    .amount(new BigDecimal("100000")).build()));
            when(requests.findById(30L)).thenReturn(Optional.of(PaymentGatewayRequest.builder()
                    .paymentRequestId(30L).vnpCreateDate("20261017090000").build()));
            service = new RefundService(refunds, receipts, requests, gateway, clock,
                    new ObjectMapper(), mock(PlatformTransactionManager.class));
        }

        void time(LocalDateTime now) {
            when(clock.instant()).thenReturn(now.toInstant(ZoneOffset.UTC));
        }

        VnpayRefundResponseDTO dispatch() {
            return gateway.refundTransaction("order_scope", new BigDecimal("50000"), "03", "payment_scope",
                    "20261017090000", "system", "Hoan tien don #5", "127.0.0.1");
        }

        void verifySingleDispatch() {
            verify(gateway).refundTransaction("order_scope", new BigDecimal("50000"), "03", "payment_scope",
                    "20261017090000", "system", "Hoan tien don #5", "127.0.0.1");
            verifyNoMoreInteractions(gateway);
        }
    }
}
