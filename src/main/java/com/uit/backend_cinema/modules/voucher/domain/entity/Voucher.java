package com.uit.backend_cinema.modules.voucher.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Voucher {
    private long voucherId;
    private String code;
    private VoucherType type;
    private BigDecimal value;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderValue;
    private int quantity;
    private int usedCount;
    private LocalDateTime startAt;
    private LocalDateTime expiredAt;
    private Boolean isActive = true;
    private Boolean isDeleted = false;
}
