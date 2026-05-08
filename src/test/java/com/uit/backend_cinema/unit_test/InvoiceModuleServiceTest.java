package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.invoice.domain.entity.Invoice;
import com.uit.backend_cinema.modules.invoice.domain.repository.InvoiceRepository;
import com.uit.backend_cinema.modules.invoice.domain.service.InvoiceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InvoiceModuleServiceTest {

    @Test
    @DisplayName("Invoice module: createInvoice should create a correct Invoice domain entity and save it")
    void createInvoiceShouldSaveDomainEntity() {
        // Arrange
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        InvoiceService invoiceService = new InvoiceService(invoiceRepository);
        
        Long bookingId = 100L;
        String paymentMethod = "VNPAY";
        BigDecimal amountPaid = new BigDecimal("50000");

        // Act
        invoiceService.createInvoice(bookingId, paymentMethod, amountPaid);

        // Assert
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        
        Invoice savedInvoice = invoiceCaptor.getValue();
        assertEquals(bookingId, savedInvoice.getBookingId());
        assertEquals(paymentMethod, savedInvoice.getPaymentMethod());
        assertEquals(amountPaid, savedInvoice.getAmountPaid());
    }
}
