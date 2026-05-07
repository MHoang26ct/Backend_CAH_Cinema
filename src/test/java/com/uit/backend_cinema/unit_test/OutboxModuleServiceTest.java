package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventStatus;
import com.uit.backend_cinema.modules.outbox.domain.repository.OutboxEventRepository;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxModuleServiceTest {

    @Test
    @DisplayName("Outbox module: claim event chuyển PENDING/RETRY sang PROCESSING")
    void claimDueEventsMarksEventsProcessing() {
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        OutboxEventService outboxEventService = new OutboxEventService(outboxEventRepository);
        OutboxEvent event = new OutboxEvent();
        event.setOutboxEventId(99L);
        event.setStatus(OutboxEventStatus.PENDING);
        event.setLastError("old error");

        when(outboxEventRepository.claimDueEvents(anyList(), any(LocalDateTime.class), eq(10))).thenReturn(List.of(event));

        List<OutboxEvent> claimed = outboxEventService.claimDueEvents(10);

        assertEquals(List.of(event), claimed);
        assertEquals(OutboxEventStatus.PROCESSING, event.getStatus());
        assertNull(event.getLastError());
        verify(outboxEventRepository).save(event);
    }
}
