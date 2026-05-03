package com.uit.backend_cinema.modules.voucher.api.entity;

import com.uit.backend_cinema.modules.voucher.domain.entity.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateVoucherRequestDTO {
    @NotBlank(message = "Mã voucher không được trống")
    private String code;

    @NotNull(message = "Loại voucher không được trống")
    private VoucherType type;

    @NotNull(message = "Giá trị voucher không được trống")
    @Positive(message = "Giá trị voucher phải lớn hơn 0")
    private BigDecimal value;

    @DecimalMin(value = "0.0", message = "Giá trị giảm giá tối đa không được bé hơn 0")
    private BigDecimal maxDiscount;

    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng tối thiểu không được bé hơn 0")
    private BigDecimal minOrderValue;

    @NotNull(message = "Số lượng voucher không được trống")
    @Positive(message = "Số lượng voucher không được bé hơn 0")
    private Integer quantity;

    @NotNull(message = "Ngày bắt đầu không được trống")
    private LocalDateTime startAt;

    @NotNull(message = "Ngày hết hạn không được trống")
    private LocalDateTime expiredAt;
}
