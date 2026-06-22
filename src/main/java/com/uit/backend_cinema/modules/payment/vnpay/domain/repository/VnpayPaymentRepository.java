package com.uit.backend_cinema.modules.payment.vnpay.domain.repository;

import com.uit.backend_cinema.modules.payment.domain.entity.PaymentGatewayRequest;
import com.uit.backend_cinema.modules.payment.domain.repository.PaymentGatewayRepository;

/**
 * Domain Repository interface cho VNPay.
 * Kế thừa contract chung để các service có thể xử lý đồng bộ.
 */
public interface VnpayPaymentRepository extends PaymentGatewayRepository {

    /**
     * Tạo payment URL cho VNPay với các tham số đặc thù: ipAddr và bankCode.
     * 
     * @param request thông tin request chung
     * @param ipAddr IP của khách hàng (bắt buộc bởi VNPay)
     * @param bankCode mã ngân hàng chọn trước (tùy chọn)
     * @return PaymentGatewayRequest đã được cập nhật payUrl và các thông tin phản hồi
     */
    PaymentGatewayRequest createVnpayPayment(PaymentGatewayRequest request, String ipAddr, String bankCode);
}
