package com.uit.backend_cinema.modules.voucher.api.internal;

import com.uit.backend_cinema.modules.voucher.api.entity.BasicVoucherDTO;
import com.uit.backend_cinema.modules.voucher.api.mapper.VoucherApiMapper;
import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherUseCase {
    private final VoucherService voucherService;
    private final VoucherApiMapper mapper;
    public VoucherUseCase(VoucherService voucherService, VoucherApiMapper mapper) {
        this.mapper = mapper;
        this.voucherService = voucherService;
    }

    public BasicVoucherDTO getVoucherById(Long voucherId) {
        Voucher voucher = voucherService.findById(voucherId);
        return mapper.toBasicDTO(voucher);
    }

    public void useVoucher(Long voucherId) {
        voucherService.useVoucher(voucherId);
    }
}
