package com.uit.backend_cinema.modules.ticket.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInRequestDTO;
import com.uit.backend_cinema.modules.ticket.api.dto.CheckInResponseDTO;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketCheckInService;

@RestController
@RequestMapping("/api/v1/staff/tickets")
public class StaffCheckInController {

    private final TicketCheckInService checkInService;

    public StaffCheckInController(TicketCheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> checkIn(@Valid @RequestBody CheckInRequestDTO requestDTO) {
        CheckInResponseDTO response = checkInService.checkIn(requestDTO);
        return ResponseEntity.ok(ApiResponse.success(response, "Check-in vé thành công"));
    }
}
