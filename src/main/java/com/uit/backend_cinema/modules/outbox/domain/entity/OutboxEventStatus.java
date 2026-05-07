package com.uit.backend_cinema.modules.outbox.domain.entity;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    RETRY,
    DONE,
    FAILED
}
