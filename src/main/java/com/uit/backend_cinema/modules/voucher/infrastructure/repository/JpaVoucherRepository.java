package com.uit.backend_cinema.modules.voucher.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.voucher.infrastructure.entity.VoucherJpaEntity;

@Repository
public interface JpaVoucherRepository extends JpaRepository<VoucherJpaEntity, Long> {
    Boolean existsByCode(String code);

    Slice<VoucherJpaEntity> findAllBy(Pageable pageable);

    @Query("""
            SELECT v FROM VoucherJpaEntity v
            WHERE v.isActive = true
                AND v.isDeleted = false
                AND v.quantity > v.usedCount
                AND v.startAt <= :now
                AND v.expiredAt > :now
            """)
    List<VoucherJpaEntity> findAllForUser(@Param("now") LocalDateTime now);

    @Modifying
    @Query("""
    update VoucherJpaEntity v
    set v.usedCount = v.usedCount + 1
    where v.voucherId = :voucherId
      and v.isActive = true
      and v.isDeleted = false
      and v.startAt <= :now
      and v.expiredAt >= :now
      and v.usedCount < v.quantity
""")
    int consumeVoucherAtomically(@Param("voucherId") Long voucherId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
    update VoucherJpaEntity v
    set v.usedCount = v.usedCount - 1
    where v.voucherId = :voucherId
      and v.usedCount > 0
""")
    int releaseVoucherAtomically(@Param("voucherId") Long voucherId);
}
