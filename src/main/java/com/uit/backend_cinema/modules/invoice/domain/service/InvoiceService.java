package com.uit.backend_cinema.modules.invoice.domain.service;

import com.uit.backend_cinema.modules.invoice.domain.entity.Invoice;
import com.uit.backend_cinema.modules.invoice.domain.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public void createInvoice(Long bookingId, String paymentMethod, BigDecimal amountPaid) {
        Invoice invoice = Invoice.builder()
                .bookingId(bookingId)
                .paymentMethod(paymentMethod)
                .amountPaid(amountPaid)
                .build();
        invoiceRepository.save(invoice);
    }
}
