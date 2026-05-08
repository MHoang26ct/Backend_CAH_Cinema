package com.uit.backend_cinema.modules.invoice.infrastructure.persistence;

import com.uit.backend_cinema.modules.invoice.domain.entity.Invoice;
import com.uit.backend_cinema.modules.invoice.domain.repository.InvoiceRepository;
import com.uit.backend_cinema.modules.invoice.infrastructure.entity.InvoiceJpaEntity;
import com.uit.backend_cinema.modules.invoice.infrastructure.mapper.InvoiceInfraMapper;
import com.uit.backend_cinema.modules.invoice.infrastructure.repository.JpaInvoiceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRepositoryImpl implements InvoiceRepository {
    private final JpaInvoiceRepository jpaInvoiceRepository;
    private final InvoiceInfraMapper invoiceInfraMapper;

    public InvoiceRepositoryImpl(JpaInvoiceRepository jpaInvoiceRepository, InvoiceInfraMapper invoiceInfraMapper) {
        this.jpaInvoiceRepository = jpaInvoiceRepository;
        this.invoiceInfraMapper = invoiceInfraMapper;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity entity = invoiceInfraMapper.toEntity(invoice);
        InvoiceJpaEntity savedEntity = jpaInvoiceRepository.save(entity);
        return invoiceInfraMapper.toDomain(savedEntity);
    }
}
