package com.uit.backend_cinema.modules.payment.api.controller;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.payment.api.dto.ClaimRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.api.dto.ConfirmCashPayoutResponseDTO;
import com.uit.backend_cinema.modules.payment.domain.service.RefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/refunds")
public class StaffRefundController {

    private final RefundService refundService;

    public StaffRefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<ApiResponse<ClaimRefundResponseDTO>> claimCashRefund(
            @PathVariable("id") Long refundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ClaimRefundResponseDTO response = refundService.claimCashRefund(refundId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/confirm-cash-payout")
    public ResponseEntity<ApiResponse<ConfirmCashPayoutResponseDTO>> confirmCashPayout(
            @PathVariable("id") Long refundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ConfirmCashPayoutResponseDTO response = refundService.confirmCashPayout(refundId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
