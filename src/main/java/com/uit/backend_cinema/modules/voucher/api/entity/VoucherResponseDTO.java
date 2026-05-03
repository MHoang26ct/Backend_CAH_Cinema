package com.uit.backend_cinema.modules.voucher.api.entity;

import com.uit.backend_cinema.modules.voucher.domain.entity.VoucherType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherResponseDTO {
    private Long voucherId;
    private String code;
    private VoucherType type;
    private BigDecimal value;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderValue;
    private Integer quantity;
    private Integer usedCount;
    private LocalDateTime startAt;
    private LocalDateTime expiredAt;
    private Boolean isActive;
    private Boolean isDeleted;
}
