package com.uit.backend_cinema.modules.outbox.infrastructure.mapper;

import java.time.LocalDateTime;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.infrastructure.entity.OutboxEventJpaEntity;

@Mapper(componentModel = "spring")
public interface OutboxInfraMapper {
    OutboxEvent toDomain(OutboxEventJpaEntity entity);

    OutboxEventJpaEntity toEntity(OutboxEvent event);

    @AfterMapping
    default void normalize(OutboxEvent event, @MappingTarget OutboxEventJpaEntity entity) {
        if (event.getRetryCount() == null) {
            entity.setRetryCount(0);
        }
        if (event.getNextRetryAt() == null) {
            entity.setNextRetryAt(LocalDateTime.now());
        }
    }
}
