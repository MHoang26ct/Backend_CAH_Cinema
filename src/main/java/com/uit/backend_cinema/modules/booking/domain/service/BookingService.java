package com.uit.backend_cinema.modules.booking.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.booking.api.dto.CreateBookingRequestDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.*;
import com.uit.backend_cinema.modules.food_order.api.entity.FoodOrderItemRequestDTO;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingFoodDraftItemRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingVoucherHoldRepository;
import com.uit.backend_cinema.modules.booking.domain.repository.PendingTicketItemRepository;
import com.uit.backend_cinema.modules.food_order.domain.entity.Food;
import com.uit.backend_cinema.modules.food_order.domain.service.FoodService;
import com.uit.backend_cinema.modules.price_config.domain.service.PriceConfigService;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import com.uit.backend_cinema.modules.voucher.domain.entity.VoucherType;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private static final long CHECKOUT_TTL_MINUTES = 15L;
    private static final int PURGE_RETENTION_DAYS = 30;

    private final BookingRepository bookingRepository;
    private final PendingTicketItemRepository pendingTicketItemRepository;
    private final BookingFoodDraftItemRepository bookingFoodDraftItemRepository;
    private final BookingVoucherHoldRepository bookingVoucherHoldRepository;
    private final SeatService seatService;
    private final ShowtimeService showtimeService;
    private final PriceConfigService priceConfigService;
    private final FoodService foodService;
    private final VoucherService voucherService;

    public BookingService(BookingRepository bookingRepository,
                          PendingTicketItemRepository pendingTicketItemRepository,
                          BookingFoodDraftItemRepository bookingFoodDraftItemRepository,
                          BookingVoucherHoldRepository bookingVoucherHoldRepository,
                          SeatService seatService,
                          ShowtimeService showtimeService,
                          PriceConfigService priceConfigService,
                          FoodService foodService,
                          VoucherService voucherService) {
        this.bookingRepository = bookingRepository;
        this.pendingTicketItemRepository = pendingTicketItemRepository;
        this.bookingFoodDraftItemRepository = bookingFoodDraftItemRepository;
        this.bookingVoucherHoldRepository = bookingVoucherHoldRepository;
        this.seatService = seatService;
        this.showtimeService = showtimeService;
        this.priceConfigService = priceConfigService;
        this.foodService = foodService;
        this.voucherService = voucherService;
    }

    @Transactional
    public PrePaymentBookingQuote createPrePaymentBooking(Long userId, CreateBookingRequestDTO requestDTO) {
        Showtime showtime = showtimeService.getById(requestDTO.getShowtimeId());
        List<Long> seatIds = requestDTO.getSeatIds();
        List<Seat> selectedSeats = seatService.promoteLocksForCheckout(requestDTO.getShowtimeId(), seatIds, showtime.getRoomId(), userId);

        try {
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CHECKOUT_TTL_MINUTES);
            BigDecimal showtimeMultiplier = priceConfigService.getPriceMultiplier(showtime.getStartTime(), showtime.getFormat());
            BigDecimal seatSubtotal = calculateSeatSubtotal(selectedSeats, showtime, showtimeMultiplier);

            List<BookingFoodDraftItem> foodDraftItems = buildFoodDraftItems(normalizeFoodItems(requestDTO.getFoodItems()));
            BigDecimal foodSubtotal = calculateFoodSubtotal(foodDraftItems);

            BigDecimal subtotal = seatSubtotal.add(foodSubtotal);
            Booking booking = createInitialBooking(userId, requestDTO, subtotal, expiresAt);

            persistPendingTicketItems(booking.getBookingId(), selectedSeats, showtime, showtimeMultiplier);
            persistFoodDraftItems(booking.getBookingId(), foodDraftItems);

            BigDecimal discountAmount = applyVoucherHold(booking.getBookingId(), requestDTO.getVoucherId(), subtotal, expiresAt);
            Booking finalizedBooking = finalizeBookingAmount(booking, subtotal, discountAmount);
            return buildQuote(finalizedBooking, seatSubtotal, foodSubtotal, discountAmount);
        } catch (RuntimeException ex) {
            seatService.releaseSeatLocksByOwner(requestDTO.getShowtimeId(), seatIds, userId);
            throw ex;
        }
    }

    @Scheduled(fixedDelayString = "${booking.prepayment.expiry-check-ms:60000}")
    @Transactional
    public void expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findByStatusAndExpiresAtBefore(BookingStatus.PENDING, now);
        for (Booking booking : expiredBookings) {
            expireSingleBooking(booking);
        }
    }

    @Scheduled(fixedDelayString = "${booking.prepayment.purge-interval-ms:21600000}")
    @Transactional
    public void purgeSoftDeletedDrafts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(PURGE_RETENTION_DAYS);
        pendingTicketItemRepository.hardDeleteSoftDeletedBefore(threshold);
        bookingFoodDraftItemRepository.hardDeleteSoftDeletedBefore(threshold);
    }

    private BigDecimal calculateSeatSubtotal(List<Seat> selectedSeats, Showtime showtime, BigDecimal showtimeMultiplier) {
        return selectedSeats.stream()
                .map(seat -> seatPrice(seat, showtime, showtimeMultiplier))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal seatPrice(Seat seat, Showtime showtime, BigDecimal showtimeMultiplier) {
        return seat.getSeatType().getPriceMultiplier()
                .multiply(showtime.getBasePrice())
                .multiply(showtimeMultiplier);
    }

    private BigDecimal calculateFoodSubtotal(List<BookingFoodDraftItem> foodDraftItems) {
        return foodDraftItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Booking createInitialBooking(Long userId, CreateBookingRequestDTO requestDTO, BigDecimal subtotal, LocalDateTime expiresAt) {
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

    private void persistPendingTicketItems(Long bookingId, List<Seat> selectedSeats, Showtime showtime, BigDecimal showtimeMultiplier) {
        List<PendingTicketItem> pendingTicketItems = selectedSeats.stream().map(seat -> {
            PendingTicketItem item = new PendingTicketItem();
            item.setBookingId(bookingId);
            item.setSeatId(seat.getSeatId());
            item.setUnitPrice(seatPrice(seat, showtime, showtimeMultiplier));
            item.setIsDeleted(false);
            return item;
        }).toList();
        pendingTicketItemRepository.saveAll(pendingTicketItems);
    }

    private void persistFoodDraftItems(Long bookingId, List<BookingFoodDraftItem> foodDraftItems) {
        List<BookingFoodDraftItem> bookingFoodDraftItems = foodDraftItems.stream().map(item -> {
            item.setBookingId(bookingId);
            return item;
        }).toList();
        bookingFoodDraftItemRepository.saveAll(bookingFoodDraftItems);
    }

    private BigDecimal applyVoucherHold(Long bookingId, Long voucherId, BigDecimal subtotal, LocalDateTime expiresAt) {
        if (voucherId == null) {
            return BigDecimal.ZERO;
        }
        Voucher voucher = validateVoucherForHold(voucherId, subtotal);
        BigDecimal discountAmount = calculateDiscount(voucher, subtotal);

        BookingVoucherHold hold = new BookingVoucherHold();
        hold.setBookingId(bookingId);
        hold.setVoucherId(voucher.getVoucherId());
        hold.setDiscountAmount(discountAmount);
        hold.setStatus(BookingVoucherHoldStatus.HELD);
        hold.setExpiresAt(expiresAt);
        hold.setIsDeleted(false);
        bookingVoucherHoldRepository.save(hold);
        return discountAmount;
    }

    private Booking finalizeBookingAmount(Booking booking, BigDecimal subtotal, BigDecimal discountAmount) {
        BigDecimal totalAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        booking.setDiscountAmount(discountAmount);
        booking.setTotalAmount(totalAmount);
        return bookingRepository.save(booking);
    }

    private PrePaymentBookingQuote buildQuote(Booking booking, BigDecimal seatSubtotal, BigDecimal foodSubtotal, BigDecimal discountAmount) {
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

    private void expireSingleBooking(Booking booking) {
        List<Long> seatIds = pendingTicketItemRepository.findAllActiveByBookingId(booking.getBookingId())
                .stream()
                .map(PendingTicketItem::getSeatId)
                .toList();
        seatService.releaseSeatLocksByOwner(booking.getShowtimeId(), seatIds, booking.getUserId());

        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        bookingVoucherHoldRepository.findByBookingId(booking.getBookingId()).ifPresent(hold -> {
            hold.setStatus(BookingVoucherHoldStatus.EXPIRED);
            bookingVoucherHoldRepository.save(hold);
        });

        pendingTicketItemRepository.softDeleteByBookingId(booking.getBookingId());
        bookingFoodDraftItemRepository.softDeleteByBookingId(booking.getBookingId());
        bookingVoucherHoldRepository.softDeleteByBookingId(booking.getBookingId());
    }

    private Voucher validateVoucherForHold(Long voucherId, BigDecimal subtotal) {
        Voucher voucher = voucherService.findById(voucherId);
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getIsActive())
                || now.isBefore(voucher.getStartAt())
                || now.isAfter(voucher.getExpiredAt())) {
            throw new BusinessException("Voucher không còn hiệu lực", ErrorCode.VALIDATION_FAILED);
        }
        if (subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BusinessException("Đơn hàng chưa đạt giá trị tối thiểu để dùng voucher", ErrorCode.VALIDATION_FAILED);
        }

        long activeHoldCount = bookingVoucherHoldRepository.countByVoucherIdAndStatusAndExpiresAtAfter(
                voucherId,
                BookingVoucherHoldStatus.HELD,
                now
        );
        long available = voucher.getQuantity() - voucher.getUsedCount() - activeHoldCount;
        if (available <= 0) {
            throw new BusinessException("Voucher đã hết lượt sử dụng", ErrorCode.VALIDATION_FAILED);
        }
        return voucher;
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        BigDecimal discount;
        if (voucher.getType() == VoucherType.FIXED_AMOUNT) {
            discount = voucher.getValue();
        } else {
            discount = subtotal.multiply(voucher.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(voucher.getMaxDiscount());
            }
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    private Map<Long, Integer> normalizeFoodItems(List<FoodOrderItemRequestDTO> foodItems) {
        if (foodItems == null || foodItems.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> foodQuantityMap = new LinkedHashMap<>();
        for (FoodOrderItemRequestDTO item : foodItems) {
            if (item.getQuantity() <= 0) {
                throw new BusinessException("Số lượng món ăn phải lớn hơn 0", ErrorCode.VALIDATION_FAILED);
            }
            foodQuantityMap.merge(item.getFoodId(), item.getQuantity(), Integer::sum);
        }
        return foodQuantityMap;
    }

    private List<BookingFoodDraftItem> buildFoodDraftItems(Map<Long, Integer> foodQuantityMap) {
        if (foodQuantityMap.isEmpty()) {
            return List.of();
        }
        List<Food> foods = foodService.findAllByListId(foodQuantityMap.keySet());
        Map<Long, Food> foodMap = foods.stream().collect(Collectors.toMap(Food::getFoodId, food -> food));
        List<BookingFoodDraftItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : foodQuantityMap.entrySet()) {
            Food food = foodMap.get(entry.getKey());
            if (food == null || !food.isAvailable()) {
                throw new BusinessException("Món ăn không khả dụng", ErrorCode.VALIDATION_FAILED);
            }
            BookingFoodDraftItem draftItem = new BookingFoodDraftItem();
            draftItem.setFoodId(entry.getKey());
            draftItem.setQuantity(entry.getValue());
            draftItem.setUnitPrice(food.getPrice());
            draftItem.setIsDeleted(false);
            items.add(draftItem);
        }
        return items;
    }
}
