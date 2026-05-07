package com.uit.backend_cinema.modules.voucher.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.voucher.domain.entity.BookingVoucherHold;
import com.uit.backend_cinema.modules.voucher.domain.entity.BookingVoucherHoldStatus;
import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import com.uit.backend_cinema.modules.voucher.domain.repository.BookingVoucherHoldRepository;
import com.uit.backend_cinema.modules.voucher.domain.repository.VoucherRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class VoucherService {
    private final VoucherRepository voucherRepository;
    private final BookingVoucherHoldRepository bookingVoucherHoldRepository;

    public VoucherService(VoucherRepository voucherRepository,
                          BookingVoucherHoldRepository bookingVoucherHoldRepository) {
        this.voucherRepository = voucherRepository;
        this.bookingVoucherHoldRepository = bookingVoucherHoldRepository;
    }

    public Voucher findById(Long voucherId) {
        return voucherRepository.findById(voucherId)
                .orElseThrow(() -> new BusinessException("Voucher không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public BigDecimal createHold(Long bookingId, Long voucherId, BigDecimal subtotal, LocalDateTime expiresAt) {
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

    public void validateHoldForPayment(Long bookingId) {
        BookingVoucherHold hold = bookingVoucherHoldRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException("Voucher hold đã hết hạn", ErrorCode.VOUCHER_HOLD_EXPIRED));
        if (hold.getStatus() != BookingVoucherHoldStatus.HELD || !hold.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Voucher hold đã hết hạn", ErrorCode.VOUCHER_HOLD_EXPIRED);
        }
    }

    @Transactional
    public void consumeHeldVoucher(Long bookingId) {
        BookingVoucherHold hold = bookingVoucherHoldRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException("Voucher hold đã hết hạn", ErrorCode.VOUCHER_HOLD_EXPIRED));
        validateHoldForPayment(bookingId);
        useVoucher(hold.getVoucherId());
        hold.setStatus(BookingVoucherHoldStatus.CONSUMED);
        bookingVoucherHoldRepository.save(hold);
    }

    @Transactional
    public void expireHold(Long bookingId) {
        bookingVoucherHoldRepository.findByBookingId(bookingId).ifPresent(hold -> {
            hold.setStatus(BookingVoucherHoldStatus.EXPIRED);
            bookingVoucherHoldRepository.save(hold);
        });
    }

    @Transactional
    public void softDeleteHold(Long bookingId) {
        bookingVoucherHoldRepository.softDeleteByBookingId(bookingId);
    }

    public Slice<Voucher> getAllForAdmin(Pageable pageable) {
        return voucherRepository.findAllForAdmin(pageable);
    }

    public List<Voucher> getAllForUser() {
        LocalDateTime now = LocalDateTime.now();
        return voucherRepository.findAllForUser(now);
    }

    @Transactional
    public Voucher create(Voucher voucher) {
        if (isValid(voucher, true, null)) {
            voucher.setUsedCount(0);
            voucher.setIsActive(true);
            voucher.setIsDeleted(false);
        }
        return voucherRepository.save(voucher);
    }

    @Transactional
    public Voucher update(Voucher newVoucher) {
        Voucher existingVoucher = voucherRepository.findById(newVoucher.getVoucherId())
                .orElseThrow(() -> new BusinessException("Voucher không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        if (isValid(newVoucher, false, existingVoucher.getCode())) {
            updateVoucher(existingVoucher, newVoucher);
            return voucherRepository.save(existingVoucher);
        }
        throw new BusinessException("Một số thông tin của voucher không hợp lệ", ErrorCode.VALIDATION_FAILED);
    }

    @Transactional
    public void useVoucher(Long id) {
        int updated = voucherRepository.consumeVoucherAtomically(id, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessException(
                    "Voucher không còn hiệu lực hoặc đã hết lượt sử dụng",
                    ErrorCode.VALIDATION_FAILED
            );
        }
    }

    @Transactional
    public void delete(Long voucherId) {
        Voucher toDelete = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new BusinessException("Voucher không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        toDelete.setIsActive(false);
        toDelete.setIsDeleted(true);
        voucherRepository.save(toDelete);
    }


    private Voucher validateVoucherForHold(Long voucherId, BigDecimal subtotal) {
        Voucher voucher = findById(voucherId);
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
        if ("FIXED_AMOUNT".equals(voucher.getType().name())) {
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

    private void updateVoucher(Voucher toUpdate, Voucher newVoucher) {
        toUpdate.setCode(newVoucher.getCode());
        toUpdate.setType(newVoucher.getType());
        toUpdate.setValue(newVoucher.getValue());
        toUpdate.setMaxDiscount(newVoucher.getMaxDiscount());
        toUpdate.setMinOrderValue(newVoucher.getMinOrderValue());
        toUpdate.setQuantity(newVoucher.getQuantity());
        toUpdate.setStartAt(newVoucher.getStartAt());
        toUpdate.setExpiredAt(newVoucher.getExpiredAt());
        toUpdate.setIsActive(newVoucher.getIsActive());
    }

    private boolean isValid(Voucher voucher, boolean isCreate, String existingCode) {
        if (isCreate) {
            if (voucherRepository.existsByCode(voucher.getCode())) {
                throw new BusinessException("Mã voucher đã tồn tại", ErrorCode.DUPLICATE_RESOURCE);
            }
        } else {
            // Khi update: chỉ check nếu code thay đổi so với code hiện tại
            if (!voucher.getCode().equals(existingCode) && voucherRepository.existsByCode(voucher.getCode())) {
                throw new BusinessException("Mã voucher đã tồn tại", ErrorCode.DUPLICATE_RESOURCE);
            }
        }
        if (voucher.getStartAt().isAfter(voucher.getExpiredAt())) {
            throw new BusinessException("Ngày bắt đầu phải trước ngày hết hạn", ErrorCode.VALIDATION_FAILED);
        }
        String type = voucher.getType().name();
        BigDecimal maxDiscount = voucher.getMaxDiscount();
        if ("PERCENT".equals(type) && (maxDiscount == null || maxDiscount.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException("Voucher giảm theo phần trăm phải có giá trị giảm tối đa lớn hơn 0", ErrorCode.VALIDATION_FAILED);
        } else if ("FIXED_AMOUNT".equals(type) && (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) != 0)) {
            throw new BusinessException("Voucher giảm theo số tiền cố định không được có giá trị giảm tối đa", ErrorCode.VALIDATION_FAILED);
        }
        return true;
    }
}
