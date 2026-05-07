package com.uit.backend_cinema.modules.outbox.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventStatus;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.repository.OutboxEventRepository;
import com.uit.backend_cinema.modules.outbox.infrastructure.entity.OutboxEventJpaEntity;
import com.uit.backend_cinema.modules.outbox.infrastructure.mapper.OutboxInfraMapper;
import com.uit.backend_cinema.modules.outbox.infrastructure.repository.JpaOutboxEventRepository;

@Repository
public class OutboxEventRepositoryImpl implements OutboxEventRepository {
    private final JpaOutboxEventRepository jpaOutboxEventRepository;
    private final OutboxInfraMapper mapper;

    public OutboxEventRepositoryImpl(JpaOutboxEventRepository jpaOutboxEventRepository, OutboxInfraMapper outboxInfraMapper) {
        this.jpaOutboxEventRepository = jpaOutboxEventRepository;
        this.mapper = outboxInfraMapper;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventJpaEntity entity = mapper.toEntity(event);
        return mapper.toDomain(jpaOutboxEventRepository.save(entity));
    }

    @Override
    public Optional<OutboxEvent> findById(Long outboxEventId) {
        return jpaOutboxEventRepository.findById(outboxEventId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<OutboxEvent> findByIdForUpdate(Long outboxEventId) {
        return jpaOutboxEventRepository.findByIdForUpdate(outboxEventId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<OutboxEvent> findByEventTypeAndAggregateId(OutboxEventType eventType, String aggregateId) {
        return jpaOutboxEventRepository.findByEventTypeAndAggregateId(eventType, aggregateId)
                .map(mapper::toDomain);
    }

    @Override
    public List<OutboxEvent> claimDueEvents(List<OutboxEventStatus> statuses, java.time.LocalDateTime now, int limit) {
        List<OutboxEventJpaEntity> entities = jpaOutboxEventRepository.findDueEventsForUpdate(now, limit);
        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<OutboxEvent> findTimedOutProcessingEvents(java.time.LocalDateTime processingBefore, int limit) {
        List<OutboxEventJpaEntity> entities = jpaOutboxEventRepository.findTimedOutProcessingEventsForUpdate(processingBefore, limit);
        return entities.stream()
                .map(mapper::toDomain)
                .toList();
    }
}
