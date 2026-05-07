package com.uit.backend_cinema.modules.outbox.domain.repository;

import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventStatus;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> findById(Long outboxEventId);

    Optional<OutboxEvent> findByIdForUpdate(Long outboxEventId);

    Optional<OutboxEvent> findByEventTypeAndAggregateId(OutboxEventType eventType, String aggregateId);

    List<OutboxEvent> claimDueEvents(List<OutboxEventStatus> statuses, LocalDateTime now, int limit);

    List<OutboxEvent> findTimedOutProcessingEvents(LocalDateTime processingBefore, int limit);
}
