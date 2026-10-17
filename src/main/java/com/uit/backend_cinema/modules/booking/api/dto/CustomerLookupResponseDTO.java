package com.uit.backend_cinema.modules.booking.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerLookupResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String rankLevel;
}
