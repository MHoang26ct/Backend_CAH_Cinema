package com.uit.backend_cinema.modules.booking.api.mapper;

import com.uit.backend_cinema.modules.booking.api.dto.CreateBookingResponseDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.PrePaymentBookingQuote;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingApiMapper {
    CreateBookingResponseDTO toResponse(PrePaymentBookingQuote quote);
}
