package com.uit.backend_cinema.modules.voucher.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import com.uit.backend_cinema.modules.voucher.domain.entity.VoucherType;
import com.uit.backend_cinema.modules.voucher.domain.repository.VoucherRepository;

@Service
@Transactional(readOnly = true)
public class VoucherService {
    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    public Voucher findById(Long voucherId) {
        return voucherRepository.findById(voucherId)
                .orElseThrow(() -> new BusinessException("Voucher không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public BigDecimal applyVoucherForBooking(Long voucherId, BigDecimal subtotal) {
        if (voucherId == null) {
            return BigDecimal.ZERO;
        }

        Voucher voucher = validateVoucherForApply(voucherId, subtotal);
        int updated = voucherRepository.consumeVoucherAtomically(voucherId, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessException("Voucher không còn hiệu lực hoặc đã hết lượt sử dụng", ErrorCode.VALIDATION_FAILED);
        }

        return calculateDiscount(voucher, subtotal);
    }

    @Transactional
    public void releaseVoucherForExpiredBooking(Long voucherId) {
        if (voucherId == null) {
            return;
        }
        voucherRepository.releaseVoucherAtomically(voucherId);
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

    private Voucher validateVoucherForApply(Long voucherId, BigDecimal subtotal) {
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
            if (!voucher.getCode().equals(existingCode) && voucherRepository.existsByCode(voucher.getCode())) {
                throw new BusinessException("Mã voucher đã tồn tại", ErrorCode.DUPLICATE_RESOURCE);
            }
        }
        if (voucher.getStartAt().isAfter(voucher.getExpiredAt())) {
            throw new BusinessException("Ngày bắt đầu phải trước ngày hết hạn", ErrorCode.VALIDATION_FAILED);
        }
        BigDecimal value = voucher.getValue();
        BigDecimal maxDiscount = voucher.getMaxDiscount();
        if (VoucherType.PERCENT.equals(voucher.getType())) {
            if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("Voucher giảm theo phần trăm phải có giá trị lớn hơn 0 và không vượt quá 100", ErrorCode.VALIDATION_FAILED);
            }
            if (maxDiscount == null || maxDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Voucher giảm theo phần trăm phải có giá trị giảm tối đa lớn hơn 0", ErrorCode.VALIDATION_FAILED);
            }
        } else if (VoucherType.FIXED_AMOUNT.equals(voucher.getType()) && (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) != 0)) {
            throw new BusinessException("Voucher giảm theo số tiền cố định không được có giá trị giảm tối đa", ErrorCode.VALIDATION_FAILED);
        }
        return true;
    }
}
