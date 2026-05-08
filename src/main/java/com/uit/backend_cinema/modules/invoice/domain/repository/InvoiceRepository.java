package com.uit.backend_cinema.modules.invoice.domain.repository;

import com.uit.backend_cinema.modules.invoice.domain.entity.Invoice;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);
}
