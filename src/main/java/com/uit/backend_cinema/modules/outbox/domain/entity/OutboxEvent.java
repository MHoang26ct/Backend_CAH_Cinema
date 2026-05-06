package com.uit.backend_cinema.modules.outbox.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OutboxEvent {
    private Long outboxEventId;
    private OutboxEventType eventType;
    private String aggregateId;
    private String payload;
    private OutboxEventStatus status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
