package com.uit.backend_cinema.modules.invoice.infrastructure.repository;

import com.uit.backend_cinema.modules.invoice.infrastructure.entity.InvoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInvoiceRepository extends JpaRepository<InvoiceJpaEntity, Long> {
}
