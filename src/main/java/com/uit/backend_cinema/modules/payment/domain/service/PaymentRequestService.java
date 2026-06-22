package com.uit.backend_cinema.modules.payment.domain.service;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentRequestRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Service dùng chung cho mọi payment gateway.
 * Xử lý persistence (save, find) — submodule service inject service này.
 */
@Service
public class PaymentRequestService {
    private final PaymentRequestRepository repository;

    public PaymentRequestService(PaymentRequestRepository repository) {
        this.repository = repository;
    }

    public PaymentGatewayRequest save(PaymentGatewayRequest request) {
        return repository.save(request);
    }

    public Optional<PaymentGatewayRequest> findLatestByBookingId(Long bookingId) {
        return repository.findLatestByBookingId(bookingId);
    }

    public Optional<PaymentGatewayRequest> findByGatewayAndOrderId(String gateway, String orderId) {
        return repository.findByGatewayAndOrderId(gateway, orderId);
    }
}
