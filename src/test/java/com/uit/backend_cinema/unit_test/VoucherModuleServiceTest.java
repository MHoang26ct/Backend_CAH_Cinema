package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.voucher.domain.entity.Voucher;
import com.uit.backend_cinema.modules.voucher.domain.entity.VoucherType;
import com.uit.backend_cinema.modules.voucher.domain.repository.VoucherRepository;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoucherModuleServiceTest {

    @Test
    @DisplayName("Voucher module: apply voucher khi tạo booking sẽ tăng used_count bằng atomic update")
    void applyVoucherForBookingConsumesVoucherAtomically() {
        VoucherRepository voucherRepository = mock(VoucherRepository.class);
        VoucherService voucherService = new VoucherService(voucherRepository);

        Voucher voucher = new Voucher();
        voucher.setVoucherId(22L);
        voucher.setType(VoucherType.FIXED_AMOUNT);
        voucher.setValue(BigDecimal.valueOf(30000));
        voucher.setMinOrderValue(BigDecimal.valueOf(100000));
        voucher.setIsActive(true);
        voucher.setIsDeleted(false);
        voucher.setStartAt(LocalDateTime.now().minusDays(1));
        voucher.setExpiredAt(LocalDateTime.now().plusDays(1));

        when(voucherRepository.findById(22L)).thenReturn(Optional.of(voucher));
        when(voucherRepository.consumeVoucherAtomically(eq(22L), any(LocalDateTime.class))).thenReturn(1);

        BigDecimal discount = voucherService.applyVoucherForBooking(22L, BigDecimal.valueOf(150000));

        assertEquals(BigDecimal.valueOf(30000), discount);
        verify(voucherRepository).consumeVoucherAtomically(eq(22L), any(LocalDateTime.class));
    }
}
