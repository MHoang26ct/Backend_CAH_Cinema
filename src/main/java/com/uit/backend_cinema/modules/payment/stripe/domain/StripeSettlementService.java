package com.uit.backend_cinema.modules.payment.stripe.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.modules.booking.domain.entity.*;
import com.uit.backend_cinema.modules.booking.domain.repository.*;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.payload.BookingPaidPayload;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.payment.domain.entity.*;
import com.uit.backend_cinema.modules.payment.domain.repository.*;
import com.uit.backend_cinema.modules.payment.domain.service.PaymentRequestService;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Uses the same receipts, refund obligations, ticket/food finalization and outbox as VNPay.
 * Called only after authenticated provider verification, inside the inbox transaction.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeSettlementService {
    private final BookingRepository bookings;
    private final PaymentReceiptRepository receipts;
    private final RefundRepository refunds;
    private final PaymentConfirmationRepository confirmations;
    private final PaymentRequestService requests;
    private final TicketService tickets;
    private final SeatService seats;
    private final FoodOrderService food;
    private final OutboxEventService outbox;
    private final ObjectMapper json;

    @Transactional(propagation = Propagation.MANDATORY)
    public void settle(StripePayment p, String intentId, LocalDateTime paidAt, LocalDateTime now) {
        Booking b = bookings.findByIdForUpdate(p.bookingId()).orElseThrow();
        if (b.getPaymentMethod() != BookingPaymentMethod.STRIPE || StripePaymentService.vnd(b.getTotalAmount()) != p.amount())
            throw new IllegalStateException("Stripe booking mismatch");
        var existing = receipts.findByGatewayAndTransactionReference("STRIPE", intentId);
        if (existing.isPresent()) {
            if (!existing.get().getBookingId().equals(b.getBookingId())) throw new IllegalStateException("Receipt ownership mismatch");
            return; // Receipt + allocation/refund were committed atomically, even with different event IDs.
        }
        PaymentReceipt receipt = receipts.save(PaymentReceipt.builder().bookingId(p.bookingId()).paymentRequestId(p.requestId())
                .gateway("STRIPE").transactionReference(intentId).orderId(p.idempotencyKey()).amount(BigDecimal.valueOf(p.amount()))
                .payDate(paidAt).verifiedAt(now).status(PaymentReceiptStatus.RECORDED).build());
        // Signed payment_intent.succeeded event time, never PaymentIntent.created.
        // Retrieval without event time conservatively uses the verification time.
        RefundCause cause = null;
        if (b.getStatus() == BookingStatus.PAID) cause = RefundCause.DUPLICATE_CHARGE;
        else if (paidAt.isAfter(p.deadline()) || b.getStatus() == BookingStatus.EXPIRED ||
                (b.getReconciliationDeadline() != null && now.isAfter(b.getReconciliationDeadline()))) cause = RefundCause.EXPIRED_LATE_PAYMENT;
        else if (b.getStatus() != BookingStatus.PENDING) cause = RefundCause.SEAT_ALLOCATION_FAILED;
        else {
            var ids = tickets.findActiveDraftSeatIds(p.bookingId());
            if (ids.isEmpty()) cause = RefundCause.SEAT_ALLOCATION_FAILED;
            else {
                try { seats.validateSeatsNotSold(b.getShowtimeId(), ids); }
                catch (com.uit.backend_cinema.common.exception.BusinessException e) { cause = RefundCause.SEAT_ALLOCATION_FAILED; }
            }
        }
        if (cause != null) {
            refunds.save(Refund.builder().paymentReceiptId(receipt.getPaymentReceiptId()).bookingId(p.bookingId())
                    .cause(cause).amount(receipt.getAmount()).status(RefundStatus.REQUESTED)
                    .requestReference("STRIPE_AUTO_" + receipt.getPaymentReceiptId()).policySnapshot("{\"refundPercent\":100}")
                    .createdAt(now).build());
        } else {
            food.finalizeDraftForBookingIfAbsent(p.bookingId());
            b.setStatus(BookingStatus.PAID);
            bookings.save(b);
            // A concurrent unique-seat conflict rolls back EVERYTHING; webhook retry then compensates.
            tickets.finalizeTicketsForPaidBooking(p.bookingId(), b.getShowtimeId());
            PaymentConfirmation confirmation = new PaymentConfirmation();
            confirmation.setBookingId(p.bookingId());
            confirmation.setGateway("STRIPE");
            confirmation.setPaymentRef(intentId);
            confirmation.setStatus(PaymentConfirmationStatus.SUCCESS);
            confirmations.save(confirmation);
            BookingPaidPayload payload = new BookingPaidPayload();
            payload.setBookingId(p.bookingId()); payload.setUserId(b.getUserId());
            payload.setShowtimeId(b.getShowtimeId()); payload.setPaymentRef(intentId);
            try { outbox.createIfAbsent(OutboxEventType.BOOKING_PAID, Long.toString(p.bookingId()), json.writeValueAsString(payload)); }
            catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize booking event"); }
        }
        var request = requests.findById(p.requestId()).orElseThrow();
        request.setStatus(PaymentGatewayRequestStatus.PAID);
        request.setGatewayTransId(intentId);
        requests.save(request);
    }
}
