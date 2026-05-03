package com.uit.backend_cinema.modules.voucher.infrastructure.mapper;

import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import com.uit.backend_cinema.modules.voucher.infrastructure.entity.VoucherJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoucherInfraMapper {
    VoucherJpaEntity toInfra(Voucher voucher);

    Voucher toDomain(VoucherJpaEntity voucherJpaEntity);
}
