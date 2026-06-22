package com.uit.backend_cinema.modules.payment.infrastructure.persistence;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentRequestRepository;
import com.uit.backend_cinema.modules.payment.infrastructure.entity.PaymentGatewayRequestJpaEntity;
import com.uit.backend_cinema.modules.payment.infrastructure.mapper.PaymentInfraMapper;
import com.uit.backend_cinema.modules.payment.infrastructure.repository.JpaPaymentGatewayRequestRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentRequestRepositoryImpl implements PaymentRequestRepository {

    private final JpaPaymentGatewayRequestRepository jpaRepository;
    private final PaymentInfraMapper mapper;

    public PaymentRequestRepositoryImpl(JpaPaymentGatewayRequestRepository jpaRepository,
                                        PaymentInfraMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentGatewayRequest save(PaymentGatewayRequest request) {
        PaymentGatewayRequestJpaEntity entity = mapper.toJpaEntity(request);
        PaymentGatewayRequestJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PaymentGatewayRequest> findLatestByBookingId(Long bookingId) {
        return jpaRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentGatewayRequest> findByGatewayAndOrderId(String gateway, String orderId) {
        return jpaRepository.findByGatewayAndOrderId(gateway, orderId)
                .map(mapper::toDomain);
    }
}
