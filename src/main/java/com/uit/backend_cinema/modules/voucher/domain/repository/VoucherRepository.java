package com.uit.backend_cinema.modules.voucher.domain.repository;

import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository {
    Optional<Voucher> findById(long voucherId);
    Boolean existsByCode(String code);
    Slice<Voucher> findAllForAdmin(Pageable pageable);
    List<Voucher> findAllForUser(LocalDateTime now);
    Voucher save(Voucher voucher);
    void delete(Voucher voucher);
    int consumeVoucherAtomically(Long voucherId, LocalDateTime now);
}
