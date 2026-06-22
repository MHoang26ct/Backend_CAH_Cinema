package com.uit.backend_cinema.modules.voucher.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.voucher.api.entity.VoucherResponseDTO;
import com.uit.backend_cinema.modules.voucher.api.mapper.VoucherApiMapper;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;

@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherUserController {
    private final VoucherApiMapper voucherApiMapper;
    private final VoucherService voucherService;

    public VoucherUserController(VoucherApiMapper voucherApiMapper, VoucherService voucherService) {
        this.voucherApiMapper = voucherApiMapper;
        this.voucherService = voucherService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<VoucherResponseDTO> response = voucherService.getAllForUser()
                .stream()
                .map(voucherApiMapper::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách voucher thành công"));
    }
}
