package com.uit.backend_cinema.integration_test;

import com.uit.backend_cinema.modules.notification.domain.repository.OtpStorage;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventStatus;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.service.BookingPaidOutboxHandler;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxWorker;
import com.uit.backend_cinema.modules.outbox.domain.service.SendTicketEmailOutboxHandler;
import com.uit.backend_cinema.modules.outbox.domain.service.ShowtimeCancelledOutboxHandler;
import com.uit.backend_cinema.modules.outbox.infrastructure.entity.OutboxEventJpaEntity;
import com.uit.backend_cinema.modules.outbox.infrastructure.repository.JpaOutboxEventRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
public class OutboxPatternIntegrationTest extends TestcontainersConfig {

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private OutboxWorker outboxWorker;

    @Autowired
    private JpaOutboxEventRepository jpaOutboxEventRepository;

    @MockBean
    private BookingPaidOutboxHandler bookingPaidOutboxHandler;

    @MockBean
    private SendTicketEmailOutboxHandler sendTicketEmailOutboxHandler;

    @MockBean
    private ShowtimeCancelledOutboxHandler showtimeCancelledOutboxHandler;

    @MockBean
    private SeatLockRepository seatLockRepository;

    @MockBean
    private OtpStorage otpStorage;

    @BeforeEach
    void cleanDb() {
        jpaOutboxEventRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("1. Ghi Nhận Sự Kiện Idempotent -> Tránh Ghi Đè Hoặc Tạo Trùng Lặp")
    void testIdempotentEventCreation() {
        String aggregateId = "100";
        String payload = "{\"bookingId\": 100}";

        // Gọi tạo lần thứ nhất
        outboxEventService.createIfAbsent(OutboxEventType.BOOKING_PAID, aggregateId, payload);

        // Gọi tạo lần thứ hai (cùng aggregateId và eventType)
        outboxEventService.createIfAbsent(OutboxEventType.BOOKING_PAID, aggregateId, "new payload duplicate");

        // Kiểm tra xem chỉ có duy nhất 1 bản ghi được tạo
        List<OutboxEventJpaEntity> allEvents = jpaOutboxEventRepository.findAll();
        assertEquals(1, allEvents.size());
        assertEquals(payload, allEvents.get(0).getPayload());
    }

    @Test
    @DisplayName("2. Lấy Các Sự Kiện Đến Hạn (Claim Due Events) -> Bỏ Qua Các Sự Kiện Trong Tương Lai")
    void testClaimDueEvents() {
        // 1. Tạo sự kiện đã quá hạn (nextRetryAt ở quá khứ) -> Sẽ được claim
        OutboxEventJpaEntity pastEvent = new OutboxEventJpaEntity();
        pastEvent.setEventType(OutboxEventType.BOOKING_PAID);
        pastEvent.setAggregateId("101");
        pastEvent.setPayload("{}");
        pastEvent.setStatus(OutboxEventStatus.PENDING);
        pastEvent.setRetryCount(0);
        pastEvent.setNextRetryAt(LocalDateTime.now().minusMinutes(5));
        jpaOutboxEventRepository.save(pastEvent);

        // 2. Tạo sự kiện chưa đến hạn (nextRetryAt ở tương lai) -> Sẽ BỊ BỎ QUA
        OutboxEventJpaEntity futureEvent = new OutboxEventJpaEntity();
        futureEvent.setEventType(OutboxEventType.SEND_TICKET_EMAIL);
        futureEvent.setAggregateId("102");
        futureEvent.setPayload("{}");
        futureEvent.setStatus(OutboxEventStatus.PENDING);
        futureEvent.setRetryCount(0);
        futureEvent.setNextRetryAt(LocalDateTime.now().plusHours(1));
        jpaOutboxEventRepository.save(futureEvent);

        // Gọi service claim các sự kiện đến hạn
        List<OutboxEvent> claimed = outboxEventService.claimDueEvents(10);

        assertEquals(1, claimed.size());
        assertEquals("101", claimed.get(0).getAggregateId());

        // Kiểm tra xem sự kiện được claim đã chuyển sang trạng thái PROCESSING chưa
        Optional<OutboxEventJpaEntity> dbPastEvent = jpaOutboxEventRepository.findById(pastEvent.getOutboxEventId());
        assertTrue(dbPastEvent.isPresent());
        assertEquals(OutboxEventStatus.PROCESSING, dbPastEvent.get().getStatus());
    }

    @Test
    @DisplayName("3. Dispatch Sự Kiện & Xử Lý Lỗi -> Tự Động Lưu Lỗi Và Tăng Số Lần Retry")
    void testWorkerDispatchErrorHandling() throws Exception {
        // Tạo sự kiện đã quá hạn (để worker lấy được)
        OutboxEventJpaEntity dueEvent = new OutboxEventJpaEntity();
        dueEvent.setEventType(OutboxEventType.BOOKING_PAID);
        dueEvent.setAggregateId("103");
        dueEvent.setPayload("{}");
        dueEvent.setStatus(OutboxEventStatus.PENDING);
        dueEvent.setRetryCount(0);
        dueEvent.setNextRetryAt(LocalDateTime.now().minusMinutes(2));
        dueEvent = jpaOutboxEventRepository.save(dueEvent);

        // Giả lập handler ném ngoại lệ khi xử lý
        doThrow(new RuntimeException("Simulated broker crash")).when(bookingPaidOutboxHandler).handle(any());

        // Gọi worker poll để quét và xử lý sự kiện
        outboxWorker.poll();

        // Kiểm tra xem sự kiện đã chuyển sang trạng thái RETRY, ghi nhận lỗi, và tăng retryCount lên 1
        Optional<OutboxEventJpaEntity> dbEvent = jpaOutboxEventRepository.findById(dueEvent.getOutboxEventId());
        assertTrue(dbEvent.isPresent());
        assertEquals(OutboxEventStatus.RETRY, dbEvent.get().getStatus());
        assertEquals(1, dbEvent.get().getRetryCount());
        assertEquals("Simulated broker crash", dbEvent.get().getLastError());
    }
}
