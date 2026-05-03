package com.uit.backend_cinema.modules.voucher.api.mapper;

import com.uit.backend_cinema.modules.voucher.api.entity.BasicVoucherDTO;
import com.uit.backend_cinema.modules.voucher.api.entity.CreateVoucherRequestDTO;
import com.uit.backend_cinema.modules.voucher.api.entity.UpdateVoucherRequestDTO;
import com.uit.backend_cinema.modules.voucher.api.entity.VoucherResponseDTO;
import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoucherApiMapper {
    @Mapping(target = "voucherId", ignore = true)
    @Mapping(target = "isActive", expression = "java(true)")
    @Mapping(target = "isDeleted", expression = "java(false)")
    @Mapping(target = "usedCount", expression = "java(0)")
    Voucher toDomain(CreateVoucherRequestDTO request);

    @Mapping(target = "usedCount", ignore = true)
    Voucher toDomain(UpdateVoucherRequestDTO request);

    BasicVoucherDTO toBasicDTO(Voucher voucher);

    VoucherResponseDTO toDTO(Voucher voucher);
}
