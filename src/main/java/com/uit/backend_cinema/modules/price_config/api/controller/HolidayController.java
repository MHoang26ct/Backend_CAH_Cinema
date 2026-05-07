package com.uit.backend_cinema.modules.price_config.api.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.price_config.api.dto.request.CreateHolidayRequest;
import com.uit.backend_cinema.modules.price_config.api.dto.request.DeleteHolidayRequest;
import com.uit.backend_cinema.modules.price_config.api.dto.request.UpdateHolidayRequest;
import com.uit.backend_cinema.modules.price_config.api.dto.response.HolidaysResponse;
import com.uit.backend_cinema.modules.price_config.api.mapper.ApiMapper;
import com.uit.backend_cinema.modules.price_config.domain.service.HolidayService;

@RestController
@RequestMapping("/api/v1/admin/holiday")
public class HolidayController {
    private final HolidayService holidayService;
    private final ApiMapper apiMapper;

    public HolidayController(HolidayService holidayService, ApiMapper apiMapper) {
        this.apiMapper = apiMapper;
        this.holidayService = holidayService;
    }

    @GetMapping("/all")
    public ResponseEntity<?> fetchAllHolidays() {
        List<HolidaysResponse> response = holidayService.fetchAllHolidays()
                .stream().map(apiMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách ngày lễ thành công"));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createHoliday(@Valid @RequestBody CreateHolidayRequest request) {
        HolidaysResponse response = apiMapper.toResponse(holidayService.createHoliday(apiMapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo ngày lễ thành công"));
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateHoliday(@Valid @RequestBody UpdateHolidayRequest request) {
        HolidaysResponse response = apiMapper.toResponse(holidayService.updateHoliday(apiMapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật ngày lễ thành công"));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteHoliday(@Valid @RequestBody DeleteHolidayRequest request) {
        holidayService.deleteHoliday(request.getHolidayId());
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa ngày lễ thành công"));
    }
}
