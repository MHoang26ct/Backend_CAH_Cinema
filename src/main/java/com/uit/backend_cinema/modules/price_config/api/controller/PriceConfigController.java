package com.uit.backend_cinema.modules.price_config.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.price_config.api.dto.request.UpdatePriceConfigRequest;
import com.uit.backend_cinema.modules.price_config.api.dto.response.PriceConfigResponse;
import com.uit.backend_cinema.modules.price_config.api.mapper.ApiMapper;
import com.uit.backend_cinema.modules.price_config.domain.service.PriceConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/price-config")
public class PriceConfigController {
    private final PriceConfigService priceConfigService;
    private final ApiMapper apiMapper;

    public PriceConfigController(PriceConfigService priceConfigService, ApiMapper apiMapper) {
        this.apiMapper = apiMapper;
        this.priceConfigService = priceConfigService;
    }

    @GetMapping("/all")
    public ResponseEntity<?> fetchAllPriceConfig() {
        List<PriceConfigResponse> response = priceConfigService.fetchAllPriceConfigs()
                .stream().map(apiMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách cấu hình giá thành thành công"));
    }

    @PostMapping("/update")
    public ResponseEntity<?> updatePriceConfig(@Valid @RequestBody UpdatePriceConfigRequest request) {
        PriceConfigResponse response = apiMapper.toResponse(priceConfigService.updatePriceConfig(apiMapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật cấu hình giá thành thành công"));
    }
}
