package com.uit.backend_cinema.modules.outbox.domain.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEvent;

@Service
public class OutboxWorker {
    private static final int BATCH_SIZE = 20;

    private final OutboxEventService outboxEventService;
    private final BookingPaidOutboxHandler bookingPaidOutboxHandler;
    private final SendTicketEmailOutboxHandler sendTicketEmailOutboxHandler;
    private final ShowtimeCancelledOutboxHandler showtimeCancelledOutboxHandler;
    private final long processingTimeoutSeconds;
    private final boolean outboxWorkerEnabled;

    public OutboxWorker(OutboxEventService outboxEventService,
                        BookingPaidOutboxHandler bookingPaidOutboxHandler,
                        SendTicketEmailOutboxHandler sendTicketEmailOutboxHandler,
                        ShowtimeCancelledOutboxHandler showtimeCancelledOutboxHandler,
                        @Value("${outbox.worker.processing-timeout-seconds:120}") long processingTimeoutSeconds,
                        @Value("${outbox.worker.enabled:true}") boolean outboxWorkerEnabled) {
        this.outboxEventService = outboxEventService;
        this.bookingPaidOutboxHandler = bookingPaidOutboxHandler;
        this.sendTicketEmailOutboxHandler = sendTicketEmailOutboxHandler;
        this.showtimeCancelledOutboxHandler = showtimeCancelledOutboxHandler;
        this.processingTimeoutSeconds = processingTimeoutSeconds;
        this.outboxWorkerEnabled = outboxWorkerEnabled;
    }

    @Scheduled(fixedDelayString = "${outbox.worker.fixed-delay-ms:5000}", initialDelayString = "${outbox.worker.initial-delay-ms:0}")
    public void poll() {
        if (!outboxWorkerEnabled) {
            return;
        }
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
                case SHOWTIME_CANCELLED -> showtimeCancelledOutboxHandler.handle(event);
            }
        } catch (Exception ex) {
            outboxEventService.markRetryOrFailed(event.getOutboxEventId(), ex);
        }
    }
}
