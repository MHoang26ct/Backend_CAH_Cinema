package com.uit.backend_cinema.modules.booking.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.common.util.VndMoney;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderItemRequestDTO;
import com.uit.backend_cinema.modules.auth.domain.entity.AccountType;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.entity.UserRank;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.auth.domain.service.UserAccountPolicy;
import com.uit.backend_cinema.modules.booking.api.dto.CreateStaffBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.api.dto.CustomerLookupResponseDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingPaymentMethod;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingSource;
import com.uit.backend_cinema.modules.booking.domain.entity.CustomerType;
import com.uit.backend_cinema.modules.booking.domain.entity.FoodFulfillmentStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.PriceSnapshot;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceipt;
import com.uit.backend_cinema.modules.payment.domain.entity.PaymentReceiptStatus;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.uit.backend_cinema.modules.outbox.domain.payload.ShowtimeCancelledPayload;
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
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final Clock clock;

    @Value("${payment.momo.enabled:true}")
    private boolean momoEnabled = true;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
            SeatService seatService,
            ShowtimeService showtimeService,
            PriceConfigService priceConfigService,
            TicketService ticketService,
            FoodOrderService foodOrderService,
            VoucherService voucherService,
            PaymentConfirmationRepository paymentConfirmationRepository,
            PaymentReceiptRepository paymentReceiptRepository,
            OutboxEventService outboxEventService,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            Clock clock) {
        this.clock = clock;
        this.bookingRepository = bookingRepository;
        this.seatService = seatService;
        this.showtimeService = showtimeService;
        this.priceConfigService = priceConfigService;
        this.ticketService = ticketService;
        this.foodOrderService = foodOrderService;
        this.voucherService = voucherService;
        this.paymentConfirmationRepository = paymentConfirmationRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.outboxEventService = outboxEventService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    public BookingService(BookingRepository bookingRepository,
            SeatService seatService,
            ShowtimeService showtimeService,
            PriceConfigService priceConfigService,
            TicketService ticketService,
            FoodOrderService foodOrderService,
            VoucherService voucherService,
            PaymentConfirmationRepository paymentConfirmationRepository,
            OutboxEventService outboxEventService,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            Clock clock) {
        this(bookingRepository, seatService, showtimeService, priceConfigService,
             ticketService, foodOrderService, voucherService, paymentConfirmationRepository,
             null, outboxEventService, objectMapper, userRepository, clock);
    }

    public void setMomoEnabled(boolean momoEnabled) {
        this.momoEnabled = momoEnabled;
    }

    @Transactional
    public PrePaymentBookingQuote createPrePaymentBooking(Long userId, CreateBookingRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User không tồn tại", ErrorCode.UNAUTHORIZED));
        UserAccountPolicy.requireInteractive(user);

        return createBookingInternal(
                user,
                userId,
                BookingSource.ONLINE,
                null,
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                requestDTO.getShowtimeId(),
                requestDTO.getSeatIds(),
                requestDTO.getFoodItems(),
                requestDTO.getVoucherId(),
                requestDTO.getPaymentMethod()
        );
    }

    @Transactional
    public PrePaymentBookingQuote createStaffBooking(Long staffUserId, CreateStaffBookingRequestDTO requestDTO) {
        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new BusinessException("Nhân viên không tồn tại", ErrorCode.UNAUTHORIZED));
        User customer;
        String contactName;
        String contactEmail;
        String contactPhone;

        if (requestDTO.getCustomerType() == CustomerType.WALK_IN) {
            customer = userRepository.getOrCreateWalkInUser();
            contactName = requestDTO.getContactName();
            contactEmail = requestDTO.getContactEmail();
            contactPhone = requestDTO.getContactPhone();
        } else if (requestDTO.getCustomerType() == CustomerType.REGISTERED) {
            if (requestDTO.getCustomerId() == null) {
                throw new BusinessException("CustomerId không được để trống khi chọn khách hàng thành viên", ErrorCode.VALIDATION_FAILED);
            }
            customer = userRepository.findById(requestDTO.getCustomerId())
                    .orElseThrow(() -> new BusinessException("Khách hàng không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
            UserAccountPolicy.requireInteractive(customer);
            contactName = requestDTO.getContactName() != null ? requestDTO.getContactName() : customer.getName();
            contactEmail = requestDTO.getContactEmail() != null ? requestDTO.getContactEmail() : customer.getEmail();
            contactPhone = requestDTO.getContactPhone() != null ? requestDTO.getContactPhone() : customer.getPhone();
        } else {
            throw new BusinessException("Loại khách hàng không hợp lệ", ErrorCode.INVALID_CUSTOMER_TYPE);
        }

        return createBookingInternal(
                customer,
                staffUserId,
                BookingSource.COUNTER,
                staffUserId,
                contactName,
                contactEmail,
                contactPhone,
                requestDTO.getShowtimeId(),
                requestDTO.getSeatIds(),
                requestDTO.getFoodItems(),
                requestDTO.getVoucherId(),
                requestDTO.getPaymentMethod()
        );
    }

    @Transactional(readOnly = true)
    public CustomerLookupResponseDTO lookupCustomerByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email không được để trống", ErrorCode.VALIDATION_FAILED);
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new BusinessException("Không tìm thấy khách hàng với email: " + email, ErrorCode.RESOURCE_NOT_FOUND));
        UserAccountPolicy.requireInteractive(user);
        return CustomerLookupResponseDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .rankLevel(user.getRankLevel() != null ? user.getRankLevel().name() : "SILVER")
                .build();
    }

    private PrePaymentBookingQuote createBookingInternal(
            User customer,
            Long lockOwnerId,
            BookingSource bookingSource,
            Long createdByStaffId,
            String contactName,
            String contactEmail,
            String contactPhone,
            Long showtimeId,
            List<Long> seatIds,
            List<FoodOrderItemRequestDTO> foodItems,
            Long voucherId,
            BookingPaymentMethod paymentMethod) {

        if (!momoEnabled && paymentMethod == BookingPaymentMethod.MOMO) {
            throw new BusinessException("Cổng thanh toán MoMo tạm thời ngưng hỗ trợ", ErrorCode.PAYMENT_METHOD_NOT_SUPPORTED);
        }

        Showtime showtime = showtimeService.getByIdForUpdate(showtimeId);
        showtimeService.validateBookingWindow(showtime, LocalDateTime.now(clock));

        List<Seat> selectedSeats = seatService.promoteLocksForCheckout(
                showtimeId, seatIds, showtime.getRoomId(), lockOwnerId);

        try {
            LocalDateTime bookingTime = LocalDateTime.now(clock);
            showtimeService.validateBookingWindow(showtime, bookingTime);

            boolean late = bookingTime.isAfter(showtime.getStartTime().plusMinutes(15));
            if (late && voucherId != null) {
                throw new BusinessException("Giá vé vào xem muộn không áp dụng cùng voucher", ErrorCode.DISCOUNT_NOT_COMBINABLE);
            }

            LocalDateTime paymentDeadline = bookingTime.plusMinutes(CHECKOUT_TTL_MINUTES);
            LocalDateTime reconciliationDeadline = paymentDeadline.plusSeconds(30);
            LocalDateTime expiresAt = paymentDeadline;

            BigDecimal showtimeMultiplier = priceConfigService.getPriceMultiplier(showtime.getStartTime(), showtime.getFormat());
            BigDecimal seatSubtotalGross = calculateSeatSubtotal(selectedSeats, showtime, showtimeMultiplier);

            Booking booking = new Booking();
            booking.setUserId(customer.getUserId() != null ? customer.getUserId() : lockOwnerId);
            booking.setShowtimeId(showtimeId);
            booking.setVoucherId(voucherId);
            booking.setPaymentMethod(paymentMethod);
            booking.setBookingSource(bookingSource);
            booking.setCreatedByStaffId(createdByStaffId);
            booking.setContactName(contactName);
            booking.setContactEmail(contactEmail);
            booking.setContactPhone(contactPhone);
            booking.setPaymentDeadline(paymentDeadline);
            booking.setReconciliationDeadline(reconciliationDeadline);
            booking.setExpiresAt(expiresAt);
            booking.setDiscountAmount(BigDecimal.ZERO);
            booking.setLateDiscountAmount(BigDecimal.ZERO);
            booking.setTotalAmount(seatSubtotalGross);
            booking.setStatus(BookingStatus.PENDING);
            booking.setIsDeleted(false);
            booking = bookingRepository.save(booking);

            ticketService.createDraftItems(booking.getBookingId(), selectedSeats, showtime, showtimeMultiplier);
            var foodDraftItems = foodOrderService.createDraftItems(booking.getBookingId(), foodItems);
            BigDecimal foodSubtotalGross = foodOrderService.calculateDraftSubtotal(foodDraftItems);

            BigDecimal rankRate = (customer.getAccountType() == AccountType.WALK_IN || customer.getRankLevel() == null)
                    ? BigDecimal.ZERO
                    : customer.getRankLevel().getDiscountRate();
            BigDecimal factor = BigDecimal.ONE.subtract(rankRate);

            BigDecimal seatSubtotalAfterRank = seatSubtotalGross.multiply(factor);
            BigDecimal foodSubtotalAfterRank = foodSubtotalGross.multiply(factor);
            BigDecimal rankDiscount = (seatSubtotalGross.subtract(seatSubtotalAfterRank))
                    .add(foodSubtotalGross.subtract(foodSubtotalAfterRank));

            BigDecimal lateDiscount = late
                    ? seatSubtotalAfterRank.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal seatAfterLate = seatSubtotalAfterRank.subtract(lateDiscount);

            BigDecimal subtotalBeforeVoucher = seatAfterLate.add(foodSubtotalAfterRank);
            BigDecimal voucherDiscountTotal = BigDecimal.ZERO;
            BigDecimal voucherDiscountSeat = BigDecimal.ZERO;
            BigDecimal voucherDiscountFood = BigDecimal.ZERO;

            if (!late) {
                BigDecimal applied = voucherService.applyVoucherForBooking(voucherId, subtotalBeforeVoucher);
                if (applied != null && applied.compareTo(BigDecimal.ZERO) > 0) {
                    voucherDiscountTotal = applied;
                    if (subtotalBeforeVoucher.compareTo(BigDecimal.ZERO) > 0) {
                        voucherDiscountSeat = voucherDiscountTotal.multiply(seatAfterLate)
                                .divide(subtotalBeforeVoucher, 2, RoundingMode.HALF_UP)
                                .min(seatAfterLate);
                        voucherDiscountFood = voucherDiscountTotal.subtract(voucherDiscountSeat).min(foodSubtotalAfterRank);
                    }
                }
            }

            BigDecimal discountAmount = lateDiscount.add(voucherDiscountTotal);
            BigDecimal totalAmount = subtotalBeforeVoucher.subtract(voucherDiscountTotal).max(BigDecimal.ZERO);
            BigDecimal netSeatAmount = seatAfterLate.subtract(voucherDiscountSeat).max(BigDecimal.ZERO);
            BigDecimal netFoodAmount = foodSubtotalAfterRank.subtract(voucherDiscountFood).max(BigDecimal.ZERO);

            booking.setSeatSubtotalGross(seatSubtotalGross);
            booking.setFoodSubtotalGross(foodSubtotalGross);
            booking.setRankDiscountAmount(rankDiscount);
            booking.setLateDiscountAmount(lateDiscount);
            booking.setVoucherDiscountSeat(voucherDiscountSeat);
            booking.setVoucherDiscountFood(voucherDiscountFood);
            booking.setNetSeatAmount(netSeatAmount);
            booking.setNetFoodAmount(netFoodAmount);
            booking.setDiscountAmount(discountAmount);
            booking.setTotalAmount(totalAmount);
            booking.setFoodFulfillmentStatus((foodDraftItems == null || foodDraftItems.isEmpty())
                    ? FoodFulfillmentStatus.NOT_APPLICABLE
                    : FoodFulfillmentStatus.PENDING);
            booking = bookingRepository.save(booking);

            return buildQuote(booking, seatSubtotalAfterRank, foodSubtotalAfterRank, discountAmount);
        } catch (RuntimeException ex) {
            seatService.releaseSeatLocksByOwner(showtimeId, seatIds, lockOwnerId);
            throw ex;
        }
    }

    public Booking getBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        if (!isAuthorizedForBooking(userId, booking)) {
            throw new BusinessException("Không có quyền truy cập booking này", ErrorCode.FORBIDDEN);
        }
        return booking;
    }

    /**
     * Xác nhận thanh toán từ IPN callback của cổng thanh toán (server-to-server).
     * Không kiểm tra userId vì đây là server gọi server.
     * Dùng chung cho mọi gateway: MoMo, VNPay, ZaloPay, ...
     */
    @Transactional
    public ConfirmPaymentResponseDTO confirmPaymentByGateway(Long bookingId, String paymentRef, String gateway) {
        // Idempotency: nếu đã confirm rồi thì trả về kết quả cũ
        Optional<PaymentConfirmation> existing = paymentConfirmationRepository.findByPaymentRef(paymentRef);
        if (existing.isPresent()) {
            PaymentConfirmation confirmation = existing.get();
            if (!confirmation.getBookingId().equals(bookingId)) {
                throw new BusinessException("Mã tham chiếu thanh toán đã được dùng cho booking khác",
                        ErrorCode.PAYMENT_REF_DUPLICATE);
            }
            Booking existingBooking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
            if (existingBooking.getStatus() == BookingStatus.PAID) {
                createBookingPaidOutbox(existingBooking, confirmation.getPaymentRef());
            }
            return buildPaymentResponse(existingBooking, confirmation.getPaymentRef(), confirmation.getGateway());
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);

        // Validate trạng thái (không cần check userId - server-to-server)
        if (booking.getStatus() == BookingStatus.PAID) {
            throw new BusinessException("Booking đã được thanh toán", ErrorCode.PAYMENT_ALREADY_CONFIRMED);
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Booking không ở trạng thái chờ thanh toán", ErrorCode.BOOKING_INVALID_STATUS);
        }
        if (!booking.getExpiresAt().isAfter(now)) {
            throw new BusinessException("Booking đã hết hạn thanh toán", ErrorCode.BOOKING_EXPIRED);
        }

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

        if (paymentReceiptRepository != null) {
            PaymentReceipt receipt = PaymentReceipt.builder()
                    .bookingId(bookingId)
                    .gateway(gateway)
                    .transactionReference(paymentRef)
                    .orderId(bookingId.toString())
                    .amount(booking.getTotalAmount())
                    .payDate(now)
                    .verifiedAt(now)
                    .status(PaymentReceiptStatus.RECORDED)
                    .build();
            try {
                paymentReceiptRepository.save(receipt);
            } catch (Exception ex) {
                // Receipt already saved
            }
        }

        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setBookingId(bookingId);
        confirmation.setPaymentRef(paymentRef);
        confirmation.setStatus(PaymentConfirmationStatus.SUCCESS);
        confirmation.setGateway(gateway);
        try {
            paymentConfirmationRepository.save(confirmation);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Mã tham chiếu thanh toán đã tồn tại", ErrorCode.PAYMENT_REF_DUPLICATE);
        }

        createBookingPaidOutbox(booking, paymentRef);
        return buildPaymentResponse(booking, paymentRef, gateway);
    }

    @Transactional
    public ConfirmPaymentResponseDTO confirmPayment(Long userId, Long bookingId, ConfirmPaymentRequestDTO requestDTO) {
        Optional<PaymentConfirmation> existingConfirmation = paymentConfirmationRepository
                .findByPaymentRef(requestDTO.getPaymentRef());
        if (existingConfirmation.isPresent()) {
            return handleExistingPaymentConfirmation(userId, bookingId, existingConfirmation.get());
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BusinessException("Booking không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);
        validatePaymentRequest(userId, booking, now);

        if (booking.getPaymentMethod() == BookingPaymentMethod.VNPAY && "CASH".equalsIgnoreCase(requestDTO.getGateway())) {
            throw new BusinessException("Không thể xác nhận tiền mặt cho đơn hàng chọn cổng VNPay", ErrorCode.PAYMENT_METHOD_NOT_SUPPORTED);
        }

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

        if (paymentReceiptRepository != null) {
            PaymentReceipt receipt = PaymentReceipt.builder()
                    .bookingId(bookingId)
                    .gateway(requestDTO.getGateway())
                    .transactionReference(requestDTO.getPaymentRef())
                    .orderId(bookingId.toString())
                    .amount(booking.getTotalAmount())
                    .payDate(now)
                    .verifiedAt(now)
                    .status(PaymentReceiptStatus.RECORDED)
                    .build();
            try {
                paymentReceiptRepository.save(receipt);
            } catch (Exception ex) {
                // Receipt already saved
            }
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
        LocalDateTime now = LocalDateTime.now(clock);
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtLessThanEqual(BookingStatus.PENDING, now);
        for (Booking booking : expiredBookings) {
            expireSingleBooking(booking, now);
        }
    }

    @Scheduled(fixedDelayString = "${booking.prepayment.purge-interval-ms:21600000}")
    @Transactional
    public void purgeSoftDeletedDrafts() {
        LocalDateTime threshold = LocalDateTime.now(clock).minusDays(PURGE_RETENTION_DAYS);
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
        if (!isAuthorizedForBooking(userId, existingBooking)) {
            throw new BusinessException("Bạn không có quyền thanh toán cho booking này", ErrorCode.FORBIDDEN);
        }
        if (existingBooking.getStatus() == BookingStatus.PAID) {
            createBookingPaidOutbox(existingBooking, confirmation.getPaymentRef());
        }
        return buildPaymentResponse(existingBooking, confirmation.getPaymentRef(), confirmation.getGateway());
    }

    private void validatePaymentRequest(Long userId, Booking booking, LocalDateTime now) {
        if (!isAuthorizedForBooking(userId, booking)) {
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

    private boolean isAuthorizedForBooking(Long userId, Booking booking) {
        if (booking.getUserId() != null && booking.getUserId().equals(userId)) {
            return true;
        }
        if (booking.getCreatedByStaffId() != null && booking.getCreatedByStaffId().equals(userId)) {
            return true;
        }
        return userRepository.findById(userId)
                .map(u -> "STAFF".equals(u.getRole()) || "ADMIN".equals(u.getRole()) || "ROLE_STAFF".equals(u.getRole()) || "ROLE_ADMIN".equals(u.getRole()))
                .orElse(false);
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
        booking.setLateDiscountAmount(BigDecimal.ZERO);
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
                .lateDiscountAmount(booking.getLateDiscountAmount())
                .voucherDiscountAmount(discountAmount.subtract(booking.getLateDiscountAmount()))
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

    /**
     * Refund toàn bộ booking còn active (PAID/PENDING) khi admin hủy showtime.
     * - PAID  → REFUNDED + tạo outbox email thông báo
     * - PENDING → EXPIRED + giải phóng seat lock và voucher
     */
    @Transactional
    public void refundBookingsForCancelledShowtime(Long showtimeId, String reason) {
        List<Booking> activeBookings = bookingRepository.findActiveByShowtimeId(showtimeId);
        LocalDateTime now = LocalDateTime.now();

        for (Booking booking : activeBookings) {
            if (booking.getStatus() == BookingStatus.PAID) {
                booking.setStatus(BookingStatus.REFUNDED);
                bookingRepository.save(booking);

                // Giải phóng voucher đã dùng
                voucherService.releaseVoucherForExpiredBooking(booking.getVoucherId());

                // Tạo outbox event để gửi email thông báo (bất đồng bộ)
                ShowtimeCancelledPayload payload = new ShowtimeCancelledPayload();
                payload.setBookingId(booking.getBookingId());
                payload.setUserId(booking.getUserId());
                payload.setShowtimeId(showtimeId);
                payload.setReason(reason);

                try {
                    outboxEventService.createIfAbsent(
                            OutboxEventType.SHOWTIME_CANCELLED,
                            "booking-" + booking.getBookingId(),
                            objectMapper.writeValueAsString(payload)
                    );
                } catch (JsonProcessingException ex) {
                    throw new BusinessException("Không thể tạo outbox event", ErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, ex);
                }

            } else if (booking.getStatus() == BookingStatus.PENDING) {
                // Hủy booking PENDING: đánh dấu EXPIRED + giải phóng lock
                expireSingleBooking(booking, now);
            }
        }
    }
}
