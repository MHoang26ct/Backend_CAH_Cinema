package com.uit.backend_cinema.modules.auth.infrastructure.repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.auth.domain.entity.AccountType;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.UserJpaEntity;

@Repository
public interface JpaUserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByAccountType(AccountType accountType);

    /**
     * Cộng thêm {@code amount} vào total_paid, tính lại total_points
     * (1 điểm = 40,000 VNĐ) và cập nhật rank_level tương ứng.
     * Thực hiện trong 1 lần UPDATE duy nhất, tránh race condition.
     */
    @Modifying
    @Query("""
            UPDATE UserJpaEntity u SET
                u.totalPaid   = u.totalPaid + :amount,
                u.totalPoint  = CAST((u.totalPaid + :amount) / 40000 AS int),
                u.rankLevel   = CASE
                    WHEN CAST((u.totalPaid + :amount) / 40000 AS int) >= 1501 THEN 'DIAMOND'
                    WHEN CAST((u.totalPaid + :amount) / 40000 AS int) >= 501  THEN 'GOLD'
                    ELSE 'SILVER'
                END
            WHERE u.userId = :userId
            """)
    void accumulatePaidAndRecalcRank(@Param("userId") Long userId,
                                     @Param("amount") BigDecimal amount);

    /**
     * Khấu trừ {@code amount} khỏi total_paid và tính lại total_points, rank_level
     * khi đơn hàng bị hủy / hoàn tiền. Đảm bảo total_paid không âm.
     */
    @Modifying
    @Query("""
            UPDATE UserJpaEntity u SET
                u.totalPaid   = CASE WHEN (u.totalPaid - :amount) < 0 THEN 0 ELSE (u.totalPaid - :amount) END,
                u.totalPoint  = CAST(CASE WHEN (u.totalPaid - :amount) < 0 THEN 0 ELSE (u.totalPaid - :amount) END / 40000 AS int),
                u.rankLevel   = CASE
                    WHEN CAST(CASE WHEN (u.totalPaid - :amount) < 0 THEN 0 ELSE (u.totalPaid - :amount) END / 40000 AS int) >= 1501 THEN 'DIAMOND'
                    WHEN CAST(CASE WHEN (u.totalPaid - :amount) < 0 THEN 0 ELSE (u.totalPaid - :amount) END / 40000 AS int) >= 501  THEN 'GOLD'
                    ELSE 'SILVER'
                END
            WHERE u.userId = :userId
            """)
    void deductPaidAndRecalcRank(@Param("userId") Long userId,
                                 @Param("amount") BigDecimal amount);
}

