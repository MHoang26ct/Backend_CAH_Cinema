package com.uit.backend_cinema.modules.payment.domain.entity;

public enum RefundStatus {
    REQUESTED,
    PROCESSING,
    UNKNOWN,
    SUCCEEDED,
    FAILED,
    REVIEW_REQUIRED
}
