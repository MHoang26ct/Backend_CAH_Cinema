package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventStatus;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.repository.OutboxEventRepository;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRetryLogicTest {

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("1. Lần đầu retry (retryCount: 0 -> 1) cho BOOKING_PAID -> Đặt trạng thái RETRY và nextRetryAt cộng ~5 giây")
    void testFirstRetryBookingPaid() {
        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(1L);
        event.setEventType(OutboxEventType.BOOKING_PAID);
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setRetryCount(0);

        when(outboxEventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));

        Exception exception = new RuntimeException("Payment service timed out");
        outboxEventService.markRetryOrFailed(1L, exception);

        assertEquals(1, event.getRetryCount());
        assertEquals(OutboxEventStatus.RETRY, event.getStatus());
        assertEquals("Payment service timed out", event.getLastError());
        assertNotNull(event.getNextRetryAt());

        long secondsDiff = ChronoUnit.SECONDS.between(LocalDateTime.now(), event.getNextRetryAt());
        assertTrue(secondsDiff >= 4 && secondsDiff <= 6, "Expected delay of ~5 seconds, got: " + secondsDiff);

        verify(outboxEventRepository).save(event);
    }

    @Test
    @DisplayName("2. Kiểm tra các độ trễ khác nhau của các loại Event: BOOKING_PAID (lần 2 = 10s), SEND_TICKET_EMAIL (lần 1 = 60s)")
    void testBackoffDelays() {
        // BOOKING_PAID, retryCount = 1 -> nextRetry is 2. The delay for retryCount 1 is 10s.
        OutboxEvent event1 = new OutboxEvent();
        event1.setOutboxEventId(1L);
        event1.setEventType(OutboxEventType.BOOKING_PAID);
        event1.setStatus(OutboxEventStatus.PROCESSING);
        event1.setRetryCount(1);

        when(outboxEventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event1));
        outboxEventService.markRetryOrFailed(1L, new RuntimeException("Error"));

        long secondsDiff1 = ChronoUnit.SECONDS.between(LocalDateTime.now(), event1.getNextRetryAt());
        assertTrue(secondsDiff1 >= 9 && secondsDiff1 <= 11, "Expected delay of ~10 seconds, got: " + secondsDiff1);

        // SEND_TICKET_EMAIL, retryCount = 0 -> nextRetry is 1. The delay for retryCount 0 is 60s.
        OutboxEvent event2 = new OutboxEvent();
        event2.setOutboxEventId(2L);
        event2.setEventType(OutboxEventType.SEND_TICKET_EMAIL);
        event2.setStatus(OutboxEventStatus.PROCESSING);
        event2.setRetryCount(0);

        when(outboxEventRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(event2));
        outboxEventService.markRetryOrFailed(2L, new RuntimeException("Error"));

        long secondsDiff2 = ChronoUnit.SECONDS.between(LocalDateTime.now(), event2.getNextRetryAt());
        assertTrue(secondsDiff2 >= 58 && secondsDiff2 <= 62, "Expected delay of ~60 seconds, got: " + secondsDiff2);
    }

    @Test
    @DisplayName("3. Đạt số lần retry tối đa (retryCount >= 5) -> Đặt trạng thái FAILED")
    void testMaxRetryReached() {
        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(1L);
        event.setEventType(OutboxEventType.BOOKING_PAID);
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setRetryCount(4); // 5th retry

        when(outboxEventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));

        outboxEventService.markRetryOrFailed(1L, new RuntimeException("Final fail"));

        assertEquals(5, event.getRetryCount());
        assertEquals(OutboxEventStatus.FAILED, event.getStatus());
        assertEquals("Final fail", event.getLastError());

        long secondsDiff = ChronoUnit.SECONDS.between(LocalDateTime.now(), event.getNextRetryAt());
        assertTrue(secondsDiff <= 2, "Expected immediate retry / no delay on FAILED, got: " + secondsDiff);
    }

    @Test
    @DisplayName("4. Cắt bớt thông báo lỗi nếu quá dài (> 2000 ký tự)")
    void testErrorMessageTrimming() {
        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(1L);
        event.setEventType(OutboxEventType.BOOKING_PAID);
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setRetryCount(0);

        when(outboxEventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));

        StringBuilder longErrorBuilder = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            longErrorBuilder.append("a");
        }
        String longErrorMessage = longErrorBuilder.toString();

        outboxEventService.markRetryOrFailed(1L, new RuntimeException(longErrorMessage));

        assertEquals(2000, event.getLastError().length());
        assertTrue(longErrorMessage.startsWith(event.getLastError()));
    }
}
