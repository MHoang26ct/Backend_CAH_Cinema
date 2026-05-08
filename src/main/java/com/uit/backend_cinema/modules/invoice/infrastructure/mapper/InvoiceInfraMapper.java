package com.uit.backend_cinema.modules.invoice.infrastructure.mapper;

import com.uit.backend_cinema.modules.invoice.domain.entity.Invoice;
import com.uit.backend_cinema.modules.invoice.infrastructure.entity.InvoiceJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceInfraMapper {
    Invoice toDomain(InvoiceJpaEntity entity);
    InvoiceJpaEntity toEntity(Invoice domain);
}
