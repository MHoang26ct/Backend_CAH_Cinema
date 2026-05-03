package com.uit.backend_cinema.modules.voucher.infrastructure.repository;

import com.uit.backend_cinema.modules.voucher.infrastructure.entity.VoucherJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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
}
