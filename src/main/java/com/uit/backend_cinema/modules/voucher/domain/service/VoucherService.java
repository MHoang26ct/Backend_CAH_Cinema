package com.uit.backend_cinema.modules.voucher.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import com.uit.backend_cinema.modules.voucher.domain.repository.VoucherRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        Voucher toUse = voucherRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Voucher không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        if (!toUse.getIsActive() || LocalDateTime.now().isBefore(toUse.getStartAt())
                || LocalDateTime.now().isAfter(toUse.getExpiredAt())) {
            throw new BusinessException("Voucher không còn hiệu lực", ErrorCode.VALIDATION_FAILED);
        }
        if (toUse.getUsedCount() >= toUse.getQuantity()) {
            throw new BusinessException("Voucher đã hết lượt sử dụng", ErrorCode.VALIDATION_FAILED);
        }
        toUse.setUsedCount(toUse.getUsedCount() + 1);
        voucherRepository.save(toUse);
    }

    @Transactional
    public void delete(Long voucherId) {
        Voucher toDelete = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new BusinessException("Voucher không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        toDelete.setIsActive(false);
        toDelete.setIsDeleted(true);
        voucherRepository.save(toDelete);
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
