package com.uit.backend_cinema.unit_test;

import com.stripe.model.PaymentIntent;
import com.uit.backend_cinema.modules.payment.domain.entity.*;
import com.uit.backend_cinema.modules.payment.domain.repository.*;
import com.uit.backend_cinema.modules.payment.stripe.domain.*;
import com.uit.backend_cinema.modules.payment.stripe.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StripeRefundProcessorTest {
    RefundRepository repo;
    StripeGateway gateway;
    StripePaymentService payments;
    StripeRefundProcessor processor;
    JdbcTemplate jdbc;
    Refund refund;
    PaymentReceipt receipt;
    LocalDateTime now = LocalDateTime.of(2026,10,18,12,0);

    @BeforeEach void setup() throws Exception {
        repo=mock(RefundRepository.class); gateway=mock(StripeGateway.class); payments=mock(StripePaymentService.class);
        jdbc=mock(JdbcTemplate.class); var receipts=mock(PaymentReceiptRepository.class); var store=mock(StripePaymentStore.class);
        processor=new StripeRefundProcessor(repo,receipts,store,gateway,payments,jdbc,new TransactionTemplate(mock(PlatformTransactionManager.class)));
        refund=Refund.builder().refundId(3L).paymentReceiptId(2L).bookingId(1L).amount(BigDecimal.valueOf(98000))
                .status(RefundStatus.REQUESTED).requestReference("test_refund").build();
        receipt=PaymentReceipt.builder().paymentReceiptId(2L).bookingId(1L).paymentRequestId(4L).gateway("STRIPE")
                .transactionReference("pi_test").amount(BigDecimal.valueOf(98000)).build();
        var payment=new StripePayment(1,4,"API",98000,"vnd","acct_test","key",null,"pi_test",null,"SUCCEEDED",now,now,"https://example.com","https://example.com","pm_card_visa");
        when(repo.findById(3L)).thenAnswer(a -> Optional.of(refund)); when(receipts.findById(2L)).thenReturn(Optional.of(receipt));
        when(store.find(1)).thenReturn(Optional.of(payment)); when(payments.now()).thenReturn(now);
        when(jdbc.queryForObject(anyString(),eq(BigDecimal.class),eq(2L),eq(3L))).thenReturn(BigDecimal.ZERO);
        PaymentIntent intent=new PaymentIntent(); intent.setStatus("succeeded"); when(gateway.intent("pi_test")).thenReturn(intent);
        when(gateway.refunds("pi_test")).thenReturn(List.of());
    }
    com.stripe.model.Refund result(long amount,String status) {
        var r=new com.stripe.model.Refund(); r.setId("re_test"); r.setPaymentIntent("pi_test"); r.setAmount(amount); r.setCurrency("vnd"); r.setStatus(status);
        r.setMetadata(Map.of("refund_reference","cinema-stripe-refund-3")); return r;
    }
    @ParameterizedTest @ValueSource(longs={98000,78400,49000})
    void fullAndPartialUseExactVndAndStableKey(long amount) throws Exception {
        refund.setAmount(BigDecimal.valueOf(amount)); when(gateway.refund("pi_test",amount,"cinema-stripe-refund-3")).thenReturn(result(amount,"succeeded"));
        processor.process(3); assertEquals(RefundStatus.SUCCEEDED,refund.getStatus()); assertEquals("re_test",refund.getGatewayTransactionNo());
        processor.process(3); verify(gateway,times(1)).refund("pi_test",amount,"cinema-stripe-refund-3");
    }
    @Test void pendingIsNotSuccess() throws Exception {
        when(gateway.refund(anyString(),anyLong(),anyString())).thenReturn(result(98000,"pending"));
        processor.process(3); assertEquals(RefundStatus.PROCESSING,refund.getStatus()); assertNull(refund.getProcessedAt());
    }
    @Test void unknownOutcomeQueriesBeforeRetry() throws Exception {
        refund.setStatus(RefundStatus.UNKNOWN); refund.setClaimedAt(now.minusMinutes(1));
        when(gateway.refunds("pi_test")).thenReturn(List.of(result(98000,"succeeded")));
        processor.process(3); assertEquals(RefundStatus.SUCCEEDED,refund.getStatus()); verify(gateway,never()).refund(anyString(),anyLong(),anyString());
    }
    @Test void crashBeforeSubmitReusesIdempotencyKeyWithinSafeWindow() throws Exception {
        refund.setStatus(RefundStatus.PROCESSING); refund.setClaimedAt(now.minusMinutes(1));
        when(gateway.refund("pi_test",98000,"cinema-stripe-refund-3")).thenReturn(result(98000,"succeeded"));
        processor.process(3); assertEquals(RefundStatus.SUCCEEDED,refund.getStatus());
    }
    @Test void doesNotReplayAfterProviderMayPruneKey() throws Exception {
        refund.setStatus(RefundStatus.UNKNOWN); refund.setClaimedAt(now.minusHours(24));
        processor.process(3); assertEquals(RefundStatus.REVIEW_REQUIRED,refund.getStatus()); verify(gateway,never()).refund(anyString(),anyLong(),anyString());
    }
    @Test void reservedMoneyPreventsOverRefund() throws Exception {
        when(jdbc.queryForObject(anyString(),eq(BigDecimal.class),eq(2L),eq(3L))).thenReturn(BigDecimal.ONE);
        processor.process(3); assertEquals(RefundStatus.REVIEW_REQUIRED,refund.getStatus()); verifyNoInteractions(gateway);
    }
    @Test void refusesReceiptFromAnotherBooking() {
        receipt.setBookingId(99L); processor.process(3); assertEquals(RefundStatus.REVIEW_REQUIRED,refund.getStatus()); verifyNoInteractions(gateway);
    }
    @Test void providerAmountMismatchIsNeverSuccess() throws Exception {
        when(gateway.refund(anyString(),anyLong(),anyString())).thenReturn(result(1,"succeeded"));
        processor.process(3); assertEquals(RefundStatus.UNKNOWN,refund.getStatus());
    }
    @Test void refundWebhookRecordsFailureEvenAfterSuccess() throws Exception {
        refund.setStatus(RefundStatus.SUCCEEDED); refund.setGatewayTransactionNo("re_test");
        when(gateway.refundById("re_test")).thenReturn(result(98000,"failed"));
        processor.handleEvent("re_test"); assertEquals(RefundStatus.FAILED,refund.getStatus());
    }
    @Test void foreignRefundCannotChangeInternalState() throws Exception {
        var r=result(98000,"succeeded"); r.setPaymentIntent("pi_other"); when(gateway.refundById("re_test")).thenReturn(r);
        assertThrows(IllegalStateException.class,()->processor.handleEvent("re_test")); assertEquals(RefundStatus.REQUESTED,refund.getStatus());
    }
}
