package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.payment.domain.entity.*;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import com.uit.backend_cinema.modules.payment.domain.service.PaymentRequestService;
import com.uit.backend_cinema.modules.payment.domain.service.RefundService;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.infrastructure.client.VnpayMerchantClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Recovery/dispatch tests use an in-memory transactional lock, never a provider or live database. */
class VnpayRefundRecoveryTest {
    private final RefundRepository refunds = mock(RefundRepository.class);
    private final PaymentReceiptRepository receipts = mock(PaymentReceiptRepository.class);
    private final PaymentRequestService requests = mock(PaymentRequestService.class);
    private final VnpayMerchantClient gateway = mock(VnpayMerchantClient.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final AtomicReference<Refund> stored = new AtomicReference<>();
    private final AtomicReference<Instant> instant = new AtomicReference<>(Instant.parse("2026-10-19T03:00:00Z"));
    private final Clock clock = new Clock() {
        public ZoneId getZone() { return ZoneId.of("Asia/Ho_Chi_Minh"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        public Instant instant() { return instant.get(); }
    };
    private RefundService service;

    @BeforeEach
    void setUp() {
        stored.set(Refund.builder().refundId(10L).paymentReceiptId(20L).bookingId(5L)
                .amount(new BigDecimal("50000")).status(RefundStatus.REQUESTED)
                .createdAt(now().minusHours(1)).build());
        PaymentReceipt receipt = PaymentReceipt.builder().paymentReceiptId(20L).paymentRequestId(30L)
                .bookingId(5L).gateway("VNPAY").orderId("ORDER_001")
                .transactionReference("PAYMENT_001").amount(new BigDecimal("100000")).build();
        when(receipts.findById(20L)).thenReturn(Optional.of(receipt));
        when(requests.findById(30L)).thenReturn(Optional.of(PaymentGatewayRequest.builder()
                .paymentRequestId(30L).vnpCreateDate("20261019090000").build()));
        when(refunds.findById(10L)).thenAnswer(inv -> Optional.of(copy(stored.get())));
        when(refunds.findByIdForUpdate(10L)).thenAnswer(inv -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), "row lock needs transaction");
            return Optional.of(copy(stored.get()));
        });
        when(refunds.save(any())).thenAnswer(inv -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            Refund saved = copy(inv.getArgument(0));
            stored.set(saved);
            return copy(saved);
        });
        when(refunds.findByStatus(any())).thenAnswer(inv -> stored.get().getStatus() == inv.getArgument(0)
                ? List.of(copy(stored.get())) : List.of());
        service = service(gateway);
    }

    private RefundService service(VnpayMerchantClient client) {
        return new RefundService(refunds, receipts, requests, client, clock, mapper, new LockingTransactionManager());
    }

    @ParameterizedTest
    @CsvSource({"00,05", "00,06", "94,05"})
    void pendingResponseIsRevisitedAndEventuallyEscalatedWithoutRedispatch(String code, String status) {
        when(dispatch()).thenAnswer(inv -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "network outside claim transaction");
            assertEquals(RefundStatus.PROCESSING, stored.get().getStatus(), "claim committed before dispatch");
            return response(code, status);
        });
        service.scanAndProcessOnlineRefunds();
        assertEquals(RefundStatus.PROCESSING, stored.get().getStatus());
        assertNull(stored.get().getProcessedAt());
        LocalDateTime claimedAt = stored.get().getClaimedAt();
        service.scanAndProcessOnlineRefunds();
        instant.updateAndGet(i -> i.plusSeconds(899));
        service.scanAndProcessOnlineRefunds();
        assertEquals(RefundStatus.PROCESSING, stored.get().getStatus());
        instant.updateAndGet(i -> i.plusSeconds(1));
        service.scanAndProcessOnlineRefunds();
        assertReview();
        assertEquals(claimedAt, stored.get().getClaimedAt());
        assertEquals(code, stored.get().getGatewayResponseCode());
        assertEquals("REFUND_001", stored.get().getGatewayTransactionNo());
        assertTrue(stored.get().getGatewayResponseMessage().contains("Provider pending"));
        service.scanAndProcessOnlineRefunds();
        verifySingleDispatch();
    }

    @ParameterizedTest
    @EnumSource(value = RefundStatus.class, names = {"PROCESSING", "UNKNOWN"})
    void staleClaimOrCrashBeforeSavingProviderResponseRequiresReview(RefundStatus status) {
        stored.get().setStatus(status);
        stored.get().setClaimedAt(now().minusMinutes(15));
        service.scanAndProcessOnlineRefunds();
        assertReview();
        assertTrue(stored.get().getGatewayResponseMessage().contains(status.name()));
        assertNull(stored.get().getProcessedAt());
        verifyNoInteractions(gateway);
    }

    @ParameterizedTest
    @EnumSource(value = RefundStatus.class, names = {"PROCESSING", "UNKNOWN"})
    void freshUnresolvedClaimIsNotReplayedOrPrematurelyEscalated(RefundStatus status) {
        stored.get().setStatus(status);
        stored.get().setClaimedAt(now());
        service.scanAndProcessOnlineRefunds();
        assertEquals(status, stored.get().getStatus());
        verify(refunds, never()).save(any());
        verifyNoInteractions(gateway);
    }

    @Test
    void legacyClaimWithoutTimestampUsesCreationDateNotFrequentlyUpdatedTimestamp() {
        stored.get().setStatus(RefundStatus.PROCESSING);
        stored.get().setUpdatedAt(now());
        service.scanAndProcessOnlineRefunds();
        assertReview();
        verifyNoInteractions(gateway);
    }

    @Test
    void legacyUnknownWithoutAnyTimestampDoesNotRemainStuck() {
        stored.get().setStatus(RefundStatus.UNKNOWN);
        stored.get().setCreatedAt(null);
        service.scanAndProcessOnlineRefunds();
        assertReview();
        verifyNoInteractions(gateway);
    }

    @Test
    void disabledClientStillEscalatesStaleRefunds() {
        stored.get().setStatus(RefundStatus.PROCESSING);
        stored.get().setClaimedAt(now().minusHours(1));
        service(null).scanAndProcessOnlineRefunds();
        assertReview();
        verifyNoInteractions(gateway);
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"GATEWAY_TIMEOUT", "GATEWAY_COMMUNICATION_FAILED"})
    void uncertainTransportFailureBecomesUnknownThenReviewNeverRetry(ErrorCode code) {
        when(dispatch()).thenThrow(new BusinessException("provider unavailable", code));
        service.processOnlineRefund(10L);
        assertEquals(RefundStatus.UNKNOWN, stored.get().getStatus());
        assertTrue(stored.get().getGatewayResponseMessage().contains(code.name()));
        service.scanAndProcessOnlineRefunds();
        instant.updateAndGet(i -> i.plusSeconds(900));
        service.scanAndProcessOnlineRefunds();
        assertReview();
        verifySingleDispatch();
    }

    @Test
    void unexpectedExceptionDoesNotPermitRedispatch() {
        when(dispatch()).thenThrow(new IllegalStateException("lost response"));
        service.processOnlineRefund(10L);
        assertEquals(RefundStatus.UNKNOWN, stored.get().getStatus());
        instant.updateAndGet(i -> i.plusSeconds(900));
        service.scanAndProcessOnlineRefunds();
        assertReview();
        verifySingleDispatch();
    }

    @Test
    void acceptedButUnrecognizedStatusRemainsUnknownUntilReview() {
        when(dispatch()).thenReturn(response("00", "42"));
        service.processOnlineRefund(10L);
        assertEquals(RefundStatus.UNKNOWN, stored.get().getStatus());
        instant.updateAndGet(i -> i.plusSeconds(900));
        service.scanAndProcessOnlineRefunds();
        assertReview();
        verifySingleDispatch();
    }

    @Test
    void twoConcurrentWorkersOnlyDispatchOnce() throws Exception {
        CountDownLatch enteredGateway = new CountDownLatch(1);
        CountDownLatch finishGateway = new CountDownLatch(1);
        CyclicBarrier bothReadRequested = new CyclicBarrier(2);
        when(refunds.findById(10L)).thenAnswer(inv -> {
            Refund snapshot = copy(stored.get());
            bothReadRequested.await(5, TimeUnit.SECONDS);
            return Optional.of(snapshot);
        });
        when(dispatch()).thenAnswer(inv -> {
            enteredGateway.countDown();
            assertTrue(finishGateway.await(5, TimeUnit.SECONDS));
            return response("00", "05");
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> service.processOnlineRefund(10L));
            Future<?> second = executor.submit(() -> service.processOnlineRefund(10L));
            assertTrue(enteredGateway.await(5, TimeUnit.SECONDS));
            finishGateway.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertEquals(RefundStatus.PROCESSING, stored.get().getStatus());
            verifySingleDispatch();
        } finally {
            finishGateway.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void lateProviderResultCannotOverwriteConcurrentManualReview() {
        when(dispatch()).thenAnswer(inv -> {
            instant.updateAndGet(i -> i.plusSeconds(900));
            service.scanAndProcessOnlineRefunds();
            assertReview();
            return response("00", "00");
        });
        service.processOnlineRefund(10L);
        assertReview();
        assertNull(stored.get().getProcessedAt());
        verifySingleDispatch();
    }

    @ParameterizedTest
    @EnumSource(value = RefundStatus.class, names = {"SUCCEEDED", "FAILED", "REVIEW_REQUIRED"})
    void terminalOutcomesAreNotRedispatched(RefundStatus status) {
        stored.get().setStatus(status);
        service.processOnlineRefund(10L);
        service.scanAndProcessOnlineRefunds();
        assertEquals(status, stored.get().getStatus());
        verify(refunds, never()).save(any());
        verifyNoInteractions(gateway);
    }

    @Test
    void missingOriginalPaymentDateRequiresReviewWithoutDispatch() {
        when(requests.findById(30L)).thenReturn(Optional.empty());
        service.processOnlineRefund(10L);
        assertEquals(RefundStatus.REVIEW_REQUIRED, stored.get().getStatus());
        assertTrue(stored.get().getGatewayResponseMessage().contains("vnp_CreateDate"));
        verifyNoInteractions(gateway);
    }

    @Test
    void successfulDispatchUsesOriginalPaymentDateAndFinishesOnce() {
        when(dispatch()).thenReturn(response("00", "00"));
        service.processOnlineRefund(10L);
        assertEquals(RefundStatus.SUCCEEDED, stored.get().getStatus());
        assertEquals(now(), stored.get().getProcessedAt());
        service.scanAndProcessOnlineRefunds();
        verifySingleDispatch();
    }

    @Test
    void membershipReconciliationRunsInsideOutcomeTransaction() {
        var membership = mock(com.uit.backend_cinema.modules.payment.domain.service.RefundMembershipService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "refundMembershipService", membership);
        doAnswer(inv -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(RefundStatus.SUCCEEDED, stored.get().getStatus());
            return null;
        }).when(membership).reconcile(10L);
        when(dispatch()).thenReturn(response("00", "00"));
        service.processOnlineRefund(10L);
        verify(membership).reconcile(10L);
    }

    @Test
    void cannotDispatchWithoutDurableTransactionManager() {
        RefundService unconfigured = new RefundService(refunds, receipts, requests, gateway, clock, mapper);
        assertThrows(IllegalStateException.class, () -> unconfigured.processOnlineRefund(10L));
        verifyNoInteractions(gateway);
    }

    private void assertReview() {
        assertEquals(RefundStatus.REVIEW_REQUIRED, stored.get().getStatus());
        assertTrue(stored.get().getGatewayResponseMessage().contains("No safe per-refund query"));
        assertTrue(stored.get().getGatewayResponseMessage().contains("do not resend"));
    }

    private VnpayRefundResponseDTO dispatch() {
        return gateway.refundTransaction("ORDER_001", new BigDecimal("50000"), "03", "PAYMENT_001",
                "20261019090000", "system", "Hoan tien don #5", "127.0.0.1");
    }

    private void verifySingleDispatch() {
        verify(gateway, times(1)).refundTransaction("ORDER_001", new BigDecimal("50000"), "03", "PAYMENT_001",
                "20261019090000", "system", "Hoan tien don #5", "127.0.0.1");
        verifyNoMoreInteractions(gateway); // In particular, no querydr on the original payment.
    }

    private VnpayRefundResponseDTO response(String code, String status) {
        return VnpayRefundResponseDTO.builder().responseCode(code).transactionStatus(status)
                .transactionType("03").transactionNo("REFUND_001").message("Provider pending").build();
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private Refund copy(Refund refund) { return mapper.convertValue(refund, Refund.class); }

    /** Models committed row-lock serialization; production uses JPA PESSIMISTIC_WRITE. */
    private static class LockingTransactionManager extends AbstractPlatformTransactionManager {
        private final ReentrantLock lock = new ReentrantLock();
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            lock.lock();
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
        @Override protected void doCleanupAfterCompletion(Object transaction) { lock.unlock(); }
    }
}
