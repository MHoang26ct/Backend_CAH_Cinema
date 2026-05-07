package com.uit.backend_cinema.modules.voucher.api.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uit.backend_cinema.modules.voucher.domain.entity.VoucherType;
import lombok.Data;

@Data
public class BasicVoucherDTO {
    private VoucherType type;
    private BigDecimal value;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderValue;
    private Integer quantity;
    private Integer usedCount;
    private LocalDateTime startAt;
    private LocalDateTime expiredAt;
}
