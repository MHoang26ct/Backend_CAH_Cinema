package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.voucher.domain.entity.BookingVoucherHold;
import com.uit.backend_cinema.modules.voucher.domain.entity.BookingVoucherHoldStatus;
import com.uit.backend_cinema.modules.voucher.domain.repository.BookingVoucherHoldRepository;
import com.uit.backend_cinema.modules.voucher.domain.repository.VoucherRepository;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("Voucher module: consume held voucher sẽ dùng voucher và đổi hold thành CONSUMED")
    void consumeHeldVoucherConsumesVoucherAndHold() {
        VoucherRepository voucherRepository = mock(VoucherRepository.class);
        BookingVoucherHoldRepository bookingVoucherHoldRepository = mock(BookingVoucherHoldRepository.class);
        VoucherService voucherService = new VoucherService(voucherRepository, bookingVoucherHoldRepository);
        BookingVoucherHold hold = new BookingVoucherHold();
        hold.setBookingId(11L);
        hold.setVoucherId(22L);
        hold.setStatus(BookingVoucherHoldStatus.HELD);
        hold.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(bookingVoucherHoldRepository.findByBookingId(11L)).thenReturn(Optional.of(hold));
        when(voucherRepository.consumeVoucherAtomically(eq(22L), any(LocalDateTime.class))).thenReturn(1);
        when(bookingVoucherHoldRepository.save(any(BookingVoucherHold.class))).thenAnswer(invocation -> invocation.getArgument(0));

        voucherService.consumeHeldVoucher(11L);

        assertEquals(BookingVoucherHoldStatus.CONSUMED, hold.getStatus());
        verify(voucherRepository).consumeVoucherAtomically(eq(22L), any(LocalDateTime.class));
        verify(bookingVoucherHoldRepository).save(hold);
    }
}
