package com.uit.backend_cinema.modules.booking.domain.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.booking.api.dto.ConfirmPaymentRequestDTO;
import com.uit.backend_cinema.modules.booking.api.dto.ConfirmPaymentResponseDTO;
import com.uit.backend_cinema.modules.booking.api.dto.CreateBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmation;
import com.uit.backend_cinema.modules.booking.domain.entity.PaymentConfirmationStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.PrePaymentBookingQuote;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.PaymentConfirmationRepository;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodOrderService;
import com.uit.backend_cinema.modules.outbox.domain.entity.OutboxEventType;
import com.uit.backend_cinema.modules.outbox.domain.payload.BookingPaidPayload;
import com.uit.backend_cinema.modules.outbox.domain.service.OutboxEventService;
import com.uit.backend_cinema.modules.price_config.domain.service.PriceConfigService;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;

@Service
public class BookingService {
    private static final long CHECKOUT_TTL_MINUTES = 15L;
    private static final int PURGE_RETENTION_DAYS = 30;

    private final BookingRepository bookingRepository;
    private final SeatService seatService;
    private final ShowtimeService showtimeService;
    private final PriceConfigService priceConfigService;
    private final TicketService ticketService;
    private final FoodOrderService foodOrderService;
    private final VoucherService voucherService;
    private final PaymentConfirmationRepository paymentConfirmationRepository;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;

    public BookingService(BookingRepository bookingRepository,
            SeatService seatService,
            ShowtimeService showtimeService,
            PriceConfigService priceConfigService,
            TicketService ticketService,
            FoodOrderService foodOrderService,
            VoucherService voucherService,
            PaymentConfirmationRepository paymentConfirmationRepository,
            OutboxEventService outboxEventService,
            ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.seatService = seatService;
        this.showtimeService = showtimeService;
        this.priceConfigService = priceConfigService;
        this.ticketService = ticketService;
        this.foodOrderService = foodOrderService;
        this.voucherService = voucherService;
        this.paymentConfirmationRepository = paymentConfirmationRepository;
        this.outboxEventService = outboxEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PrePaymentBookingQuote createPrePaymentBooking(Long userId, CreateBookingRequestDTO requestDTO) {
        Showtime showtime = showtimeService.getById(requestDTO.getShowtimeId());
        List<Long> seatIds = requestDTO.getSeatIds();
        List<Seat> selectedSeats = seatService.promoteLocksForCheckout(requestDTO.getShowtimeId(), seatIds,
                showtime.getRoomId(), userId);

        try {
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CHECKOUT_TTL_MINUTES);
            BigDecimal showtimeMultiplier = priceConfigService.getPriceMultiplier(showtime.getStartTime(),
                    showtime.getFormat());
            BigDecimal seatSubtotal = calculateSeatSubtotal(selectedSeats, showtime, showtimeMultiplier);
            BigDecimal subtotal = seatSubtotal;
            Booking booking = createInitialBooking(userId, requestDTO, subtotal, expiresAt);

            ticketService.createDraftItems(booking.getBookingId(), selectedSeats, showtime, showtimeMultiplier);
            var foodDraftItems = foodOrderService.createDraftItems(booking.getBookingId(), requestDTO.getFoodItems());
            BigDecimal foodSubtotal = foodOrderService.calculateDraftSubtotal(foodDraftItems);
            subtotal = seatSubtotal.add(foodSubtotal);
            booking.setTotalAmount(subtotal);
            booking = bookingRepository.save(booking);

            BigDecimal discountAmount = voucherService.applyVoucherForBooking(requestDTO.getVoucherId(), subtotal);
            Booking finalizedBooking = finalizeBookingAmount(booking, subtotal, discountAmount);
            return buildQuote(finalizedBooking, seatSubtotal, foodSubtotal, discountAmount);
        } catch (RuntimeException ex) {
            seatService.releaseSeatLocksByOwner(requestDTO.getShowtimeId(), seatIds, userId);
            throw ex;
        }
    }

    @Transactional
    public ConfirmPaymentResponseDTO confirmPayment(Long userId, Long bookingId, ConfirmPaymentRequestDTO requestDTO) {
        Optional<PaymentConfirmation> existingConfirmation = paymentConfirmationRepository
                .findByPaymentRef(requestDTO.getPaymentRef());
        if (existingConfirmation.isPresent()) {
            return handleExistingPaymentConfirmation(userId, bookingId, existingConfirmation.get());
        }

        LocalDateTime now = LocalDateTime.now();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        validatePaymentRequest(userId, booking, now);
        List<Long> seatIds = ticketService.findActiveDraftSeatIds(bookingId);
        seatService.validateSeatsNotSold(booking.getShowtimeId(), seatIds);

        foodOrderService.finalizeDraftForBookingIfAbsent(bookingId);

        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);
        try {
            ticketService.finalizeTicketsForPaidBooking(bookingId, booking.getShowtimeId());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Ghế đã được bán cho suất chiếu này", ErrorCode.SEAT_ALREADY_BOOKED, ex);
        }

        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setBookingId(bookingId);
        confirmation.setPaymentRef(requestDTO.getPaymentRef());
        confirmation.setStatus(PaymentConfirmationStatus.SUCCESS);
        confirmation.setGateway(requestDTO.getGateway());
        try {
            paymentConfirmationRepository.save(confirmation);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Mã tham chiếu thanh toán đã tồn tại", ErrorCode.PAYMENT_REF_DUPLICATE);
        }

        createBookingPaidOutbox(booking, requestDTO.getPaymentRef());
        return buildPaymentResponse(booking, requestDTO.getPaymentRef(), requestDTO.getGateway());
    }

    @Scheduled(fixedDelayString = "${booking.prepayment.expiry-check-ms:60000}")
    @Transactional
    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(BookingStatus.PENDING, now);
        for (Booking booking : expiredBookings) {
            expireSingleBooking(booking, now);
        }
    }

    @Scheduled(fixedDelayString = "${booking.prepayment.purge-interval-ms:21600000}")
    @Transactional
    public void purgeSoftDeletedDrafts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(PURGE_RETENTION_DAYS);
        ticketService.purgeSoftDeletedDraftItems(threshold);
        foodOrderService.purgeSoftDeletedDraftItems(threshold);
    }

    private ConfirmPaymentResponseDTO handleExistingPaymentConfirmation(Long userId,
            Long bookingId,
            PaymentConfirmation confirmation) {
        if (!confirmation.getBookingId().equals(bookingId)) {
            throw new BusinessException("Mã tham chiếu thanh toán đã được dùng cho booking khác",
                    ErrorCode.PAYMENT_REF_DUPLICATE);
        }
        Booking existingBooking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        if (!existingBooking.getUserId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền thanh toán cho booking này", ErrorCode.FORBIDDEN);
        }
        if (existingBooking.getStatus() == BookingStatus.PAID) {
            createBookingPaidOutbox(existingBooking, confirmation.getPaymentRef());
        }
        return buildPaymentResponse(existingBooking, confirmation.getPaymentRef(), confirmation.getGateway());
    }

    private void validatePaymentRequest(Long userId, Booking booking, LocalDateTime now) {
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền thanh toán cho booking này", ErrorCode.FORBIDDEN);
        }
        if (booking.getStatus() == BookingStatus.PAID) {
            throw new BusinessException("Booking đã được thanh toán", ErrorCode.PAYMENT_ALREADY_CONFIRMED);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Booking không ở trạng thái chờ thanh toán", ErrorCode.BOOKING_INVALID_STATUS);
        }
        if (!booking.getExpiresAt().isAfter(now)) {
            throw new BusinessException("Booking đã hết hạn thanh toán", ErrorCode.BOOKING_EXPIRED);
        }
    }

    private void createBookingPaidOutbox(Booking booking, String paymentRef) {
        BookingPaidPayload payload = new BookingPaidPayload();
        payload.setBookingId(booking.getBookingId());
        payload.setUserId(booking.getUserId());
        payload.setShowtimeId(booking.getShowtimeId());
        payload.setPaymentRef(paymentRef);

        try {
            outboxEventService.createIfAbsent(
                    OutboxEventType.BOOKING_PAID,
                    booking.getBookingId().toString(),
                    objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Không thể tạo payload BOOKING_PAID",
                    ErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, ex);
        }
    }

    private BigDecimal calculateSeatSubtotal(List<Seat> selectedSeats, Showtime showtime,
            BigDecimal showtimeMultiplier) {
        return selectedSeats.stream()
                .map(seat -> seat.getSeatType().getPriceMultiplier()
                        .multiply(showtime.getBasePrice())
                        .multiply(showtimeMultiplier))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Booking createInitialBooking(Long userId, CreateBookingRequestDTO requestDTO, BigDecimal subtotal,
            LocalDateTime expiresAt) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setShowtimeId(requestDTO.getShowtimeId());
        booking.setVoucherId(requestDTO.getVoucherId());
        booking.setPaymentMethod(requestDTO.getPaymentMethod());
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalAmount(subtotal);
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(expiresAt);
        booking.setIsDeleted(false);
        return bookingRepository.save(booking);
    }

    private Booking finalizeBookingAmount(Booking booking, BigDecimal subtotal, BigDecimal discountAmount) {
        BigDecimal totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        booking.setDiscountAmount(discountAmount);
        booking.setTotalAmount(totalAmount);
        return bookingRepository.save(booking);
    }

    private PrePaymentBookingQuote buildQuote(Booking booking, BigDecimal seatSubtotal, BigDecimal foodSubtotal,
            BigDecimal discountAmount) {
        return PrePaymentBookingQuote.builder()
                .bookingId(booking.getBookingId())
                .status(booking.getStatus())
                .expiresAt(booking.getExpiresAt())
                .seatSubtotal(seatSubtotal)
                .foodSubtotal(foodSubtotal)
                .discountAmount(discountAmount)
                .totalAmount(booking.getTotalAmount())
                .build();
    }

    private ConfirmPaymentResponseDTO buildPaymentResponse(Booking booking, String paymentRef, String gateway) {
        return ConfirmPaymentResponseDTO.builder()
                .bookingId(booking.getBookingId())
                .status(booking.getStatus())
                .paymentRef(paymentRef)
                .gateway(gateway)
                .ticketStatus("PENDING")
                .build();
    }

    private void expireSingleBooking(Booking booking, LocalDateTime now) {
        int expired = bookingRepository.markExpiredIfPendingAndExpired(booking.getBookingId(), now);
        if (expired == 0) {
            return;
        }

        List<Long> seatIds = ticketService.findActiveDraftSeatIds(booking.getBookingId());
        seatService.releaseSeatLocksByOwner(booking.getShowtimeId(), seatIds, booking.getUserId());

        voucherService.releaseVoucherForExpiredBooking(booking.getVoucherId());
        ticketService.expireDraftItems(booking.getBookingId());
        foodOrderService.expireDraftItems(booking.getBookingId());
    }
}
