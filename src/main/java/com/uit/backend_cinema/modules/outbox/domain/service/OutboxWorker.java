package com.uit.backend_cinema.modules.outbox.domain.service;

import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxWorker {
    private static final int BATCH_SIZE = 20;

    private final OutboxEventService outboxEventService;
    private final BookingPaidOutboxHandler bookingPaidOutboxHandler;
    private final SendTicketEmailOutboxHandler sendTicketEmailOutboxHandler;
    private final long processingTimeoutSeconds;

    public OutboxWorker(OutboxEventService outboxEventService,
                        BookingPaidOutboxHandler bookingPaidOutboxHandler,
                        SendTicketEmailOutboxHandler sendTicketEmailOutboxHandler,
                        @Value("${outbox.worker.processing-timeout-seconds:120}") long processingTimeoutSeconds) {
        this.outboxEventService = outboxEventService;
        this.bookingPaidOutboxHandler = bookingPaidOutboxHandler;
        this.sendTicketEmailOutboxHandler = sendTicketEmailOutboxHandler;
        this.processingTimeoutSeconds = processingTimeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${outbox.worker.fixed-delay-ms:5000}")
    public void poll() {
        outboxEventService.reclaimTimedOutProcessingEvents(processingTimeoutSeconds, BATCH_SIZE);
        List<OutboxEvent> events = outboxEventService.claimDueEvents(BATCH_SIZE);
        for (OutboxEvent event : events) {
            processOne(event);
        }
    }

    private void processOne(OutboxEvent event) {
        try {
            switch (event.getEventType()) {
                case BOOKING_PAID -> bookingPaidOutboxHandler.handle(event);
                case SEND_TICKET_EMAIL -> sendTicketEmailOutboxHandler.handle(event);
            }
        } catch (Exception ex) {
            outboxEventService.markRetryOrFailed(event.getOutboxEventId(), ex);
        }
    }
}
