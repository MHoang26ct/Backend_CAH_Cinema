package com.uit.backend_cinema.modules.voucher.api.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.voucher.api.entity.CreateVoucherRequestDTO;
import com.uit.backend_cinema.modules.voucher.api.entity.UpdateVoucherRequestDTO;
import com.uit.backend_cinema.modules.voucher.api.entity.VoucherResponseDTO;
import com.uit.backend_cinema.modules.voucher.api.mapper.VoucherApiMapper;
import com.uit.backend_cinema.modules.voucher.domain.service.VoucherService;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
public class VoucherAdminController {
    private final VoucherApiMapper voucherApiMapper;
    private final VoucherService voucherService;

    public VoucherAdminController(VoucherApiMapper voucherApiMapper, VoucherService voucherService) {
        this.voucherApiMapper = voucherApiMapper;
        this.voucherService = voucherService;
    }

    @GetMapping
    public ResponseEntity<?> getAllVouchers(
            @PageableDefault(size = 20, sort = "voucherId", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Slice<VoucherResponseDTO> response = voucherService.getAllForAdmin(pageable)
                .map(voucherApiMapper::toDTO);

        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách voucher thành công"));
    }

    @GetMapping("/{voucherId}")
    public ResponseEntity<?> getVoucherById(@PathVariable Long voucherId) {
        VoucherResponseDTO response = voucherApiMapper.toDTO(voucherService.findById(voucherId));
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy voucher với ID " + voucherId + " thành công"));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createVoucher(@Valid @RequestBody CreateVoucherRequestDTO request) {
        VoucherResponseDTO response = voucherApiMapper.toDTO(voucherService.create(voucherApiMapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo voucher thành công"));
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateVoucher(@Valid @RequestBody UpdateVoucherRequestDTO request) {
        VoucherResponseDTO response = voucherApiMapper.toDTO(voucherService.update(voucherApiMapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật voucher thành công"));
    }

    @DeleteMapping("/{voucherId}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Long voucherId) {
        voucherService.delete(voucherId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa voucher thành công"));
    }
}
