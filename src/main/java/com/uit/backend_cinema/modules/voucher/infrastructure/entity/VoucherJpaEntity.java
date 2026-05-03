package com.uit.backend_cinema.modules.voucher.infrastructure.entity;

import com.uit.backend_cinema.modules.voucher.domain.entity.VoucherType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
public class VoucherJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long voucherId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    private VoucherType type;

    @Column(name = "value", nullable = false)
    private BigDecimal value;
    @Column(name = "max_discount")
    private BigDecimal maxDiscount;

    @Column(name = "min_order_value", nullable = false)
    private BigDecimal minOrderValue;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
