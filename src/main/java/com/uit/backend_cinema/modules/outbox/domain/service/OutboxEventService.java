package com.uit.backend_cinema.modules.outbox.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventStatus;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.repository.OutboxEventRepository;

@Service
public class OutboxEventService {
    private static final int MAX_RETRY = 5;
    private static final int MAX_ERROR_LENGTH = 2000;
    private static final String PROCESSING_TIMEOUT_MESSAGE = "Reclaimed timed-out PROCESSING event";

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public void createIfAbsent(OutboxEventType eventType, String aggregateId, String payload) {
        if (outboxEventRepository.findByEventTypeAndAggregateId(eventType, aggregateId).isPresent()) {
            return;
        }

        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setAggregateId(aggregateId);
        event.setPayload(payload);
        event.setStatus(OutboxEventStatus.PENDING);
        event.setRetryCount(0);
        event.setNextRetryAt(LocalDateTime.now());

        try {
            outboxEventRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            if (outboxEventRepository.findByEventTypeAndAggregateId(eventType, aggregateId).isPresent()) {
                return;
            }
            throw new BusinessException("Không thể tạo outbox event", ErrorCode.OUTBOX_EVENT_CREATE_FAILED, ex);
        }
    }

    @Transactional
    public List<OutboxEvent> claimDueEvents(int limit) {
        List<OutboxEvent> events = outboxEventRepository.claimDueEvents(
                List.of(OutboxEventStatus.PENDING, OutboxEventStatus.RETRY),
                LocalDateTime.now(),
                limit
        );

        for (OutboxEvent event : events) {
            event.setStatus(OutboxEventStatus.PROCESSING);
            event.setLastError(null);
            outboxEventRepository.save(event);
        }

        return events;
    }

    @Transactional
    public int reclaimTimedOutProcessingEvents(long timeoutSeconds, int limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processingBefore = now.minusSeconds(timeoutSeconds);
        List<OutboxEvent> timedOutEvents = outboxEventRepository.findTimedOutProcessingEvents(processingBefore, limit);

        for (OutboxEvent event : timedOutEvents) {
            int currentRetry = event.getRetryCount() == null ? 0 : event.getRetryCount();
            int nextRetry = currentRetry + 1;

            event.setRetryCount(nextRetry);
            event.setLastError(PROCESSING_TIMEOUT_MESSAGE);

            if (nextRetry >= MAX_RETRY) {
                event.setStatus(OutboxEventStatus.FAILED);
                event.setNextRetryAt(now);
            } else {
                event.setStatus(OutboxEventStatus.RETRY);
                event.setNextRetryAt(now.plusSeconds(backoffSeconds(event.getEventType(), currentRetry)));
            }

            outboxEventRepository.save(event);
        }

        return timedOutEvents.size();
    }


    @Transactional
    public void markDone(Long outboxEventId) {
        OutboxEvent event = findEventForUpdateOrThrow(outboxEventId);
        event.setStatus(OutboxEventStatus.DONE);
        event.setLastError(null);
        event.setNextRetryAt(LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    @Transactional
    public void markRetryOrFailed(Long outboxEventId, Exception ex) {
        OutboxEvent event = findEventForUpdateOrThrow(outboxEventId);
        int currentRetry = event.getRetryCount() == null ? 0 : event.getRetryCount();
        int nextRetry = currentRetry + 1;

        event.setRetryCount(nextRetry);
        event.setLastError(trimError(ex));

        if (nextRetry >= MAX_RETRY) {
            event.setStatus(OutboxEventStatus.FAILED);
            event.setNextRetryAt(LocalDateTime.now());
        } else {
            event.setStatus(OutboxEventStatus.RETRY);
            event.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds(event.getEventType(), currentRetry)));
        }

        outboxEventRepository.save(event);
    }

    private long backoffSeconds(OutboxEventType eventType, int retryCount) {
        return switch (eventType) {
            case BOOKING_PAID -> bookingPaidBackoffSeconds(retryCount);
            case SEND_TICKET_EMAIL -> sendTicketEmailBackoffSeconds(retryCount);
            case SHOWTIME_CANCELLED -> showtimeCancelledBackoffSeconds(retryCount);
        };
    }

    private long bookingPaidBackoffSeconds(int retryCount) {
        long[] delays = {5, 10, 30, 60, 120};
        return delays[Math.min(retryCount, delays.length - 1)];
    }

    private long sendTicketEmailBackoffSeconds(int retryCount) {
        long[] delays = {60, 120, 240, 480, 960};
        return delays[Math.min(retryCount, delays.length - 1)];
    }

    private long showtimeCancelledBackoffSeconds(int retryCount) {
        long[] delays = {60, 120, 240, 480, 960};
        return delays[Math.min(retryCount, delays.length - 1)];
    }

    private OutboxEvent findEventForUpdateOrThrow(Long outboxEventId) {
        return outboxEventRepository.findByIdForUpdate(outboxEventId)
                .orElseThrow(() -> new BusinessException(
                        "Outbox event không tồn tại: " + outboxEventId,
                        ErrorCode.RESOURCE_NOT_FOUND
                ));
    }

    private String trimError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
