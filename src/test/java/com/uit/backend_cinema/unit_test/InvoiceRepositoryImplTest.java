package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.invoice.domain.entity.Invoice;
import com.uit.backend_cinema.modules.invoice.infrastructure.entity.InvoiceJpaEntity;
import com.uit.backend_cinema.modules.invoice.infrastructure.mapper.InvoiceInfraMapper;
import com.uit.backend_cinema.modules.invoice.infrastructure.persistence.InvoiceRepositoryImpl;
import com.uit.backend_cinema.modules.invoice.infrastructure.repository.JpaInvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceRepositoryImplTest {

    @Test
    @DisplayName("Invoice repository impl: save should map domain to jpa entity and return domain entity")
    void saveShouldMapAndSave() {
        // Arrange
        JpaInvoiceRepository jpaRepository = mock(JpaInvoiceRepository.class);
        InvoiceInfraMapper mapper = mock(InvoiceInfraMapper.class);
        InvoiceRepositoryImpl repository = new InvoiceRepositoryImpl(jpaRepository, mapper);

        Invoice domainInvoice = Invoice.builder()
                .bookingId(1L)
                .paymentMethod("MOMO")
                .amountPaid(new BigDecimal("10000"))
                .build();

        InvoiceJpaEntity jpaEntity = new InvoiceJpaEntity();
        jpaEntity.setBookingId(1L);
        jpaEntity.setPaymentMethod("MOMO");
        jpaEntity.setAmountPaid(new BigDecimal("10000"));

        when(mapper.toEntity(domainInvoice)).thenReturn(jpaEntity);
        when(jpaRepository.save(jpaEntity)).thenReturn(jpaEntity);
        when(mapper.toDomain(jpaEntity)).thenReturn(domainInvoice);

        // Act
        Invoice result = repository.save(domainInvoice);

        // Assert
        verify(mapper).toEntity(domainInvoice);
        verify(jpaRepository).save(jpaEntity);
        verify(mapper).toDomain(jpaEntity);
        assertEquals(domainInvoice, result);
    }
}
