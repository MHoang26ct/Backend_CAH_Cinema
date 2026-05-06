package com.uit.backend_cinema.modules.outbox.infrastructure.repository;

import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.infrastructure.entity.OutboxEventJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, Long> {

    Optional<OutboxEventJpaEntity> findByEventTypeAndAggregateId(OutboxEventType eventType, String aggregateId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEventJpaEntity e where e.outboxEventId = :outboxEventId")
    Optional<OutboxEventJpaEntity> findByIdForUpdate(@Param("outboxEventId") Long outboxEventId);

    @Query(
            value = """
                    select *
                    from outbox_events
                    where status in ('PENDING', 'RETRY')
                      and next_retry_at <= :now
                    order by created_at
                    for update skip locked
                    limit :limit
                    """,
            nativeQuery = true
    )
    List<OutboxEventJpaEntity> findDueEventsForUpdate(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
