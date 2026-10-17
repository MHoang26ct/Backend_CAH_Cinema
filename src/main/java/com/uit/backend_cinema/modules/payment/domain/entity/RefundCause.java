package com.uit.backend_cinema.modules.payment.domain.entity;

public enum RefundCause {
    CUSTOMER_CANCELLED,
    SHOWTIME_CANCELLED,
    EXPIRED_LATE_PAYMENT,
    DUPLICATE_CHARGE,
    SEAT_ALLOCATION_FAILED,
    MANUAL_STAFF
}
