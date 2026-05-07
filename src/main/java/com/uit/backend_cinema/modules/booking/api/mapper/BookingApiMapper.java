package com.uit.backend_cinema.modules.booking.api.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.booking.api.dto.CreateBookingResponseDTO;
import com.uit.backend_cinema.modules.booking.domain.entity.PrePaymentBookingQuote;

@Mapper(componentModel = "spring")
public interface BookingApiMapper {
    CreateBookingResponseDTO toResponse(PrePaymentBookingQuote quote);
}
