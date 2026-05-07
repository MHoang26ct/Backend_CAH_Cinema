package com.uit.backend_cinema.modules.price_config.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.price_config.api.dto.request.CreateHolidayRequest;
import com.uit.backend_cinema.modules.price_config.api.dto.request.UpdateHolidayRequest;
import com.uit.backend_cinema.modules.price_config.api.dto.request.UpdatePriceConfigRequest;
import com.uit.backend_cinema.modules.price_config.api.dto.response.HolidaysResponse;
import com.uit.backend_cinema.modules.price_config.api.dto.response.PriceConfigResponse;
import com.uit.backend_cinema.modules.price_config.domain.entity.Holiday;
import com.uit.backend_cinema.modules.price_config.domain.entity.PriceConfig;

@Mapper(componentModel = "spring")
public interface ApiMapper {
    PriceConfig toDomain(UpdatePriceConfigRequest priceConfigRequest);

    PriceConfigResponse toResponse(PriceConfig priceConfig);

    HolidaysResponse toResponse(Holiday holiday);

    @Mapping(target = "holidayId", ignore = true)
    Holiday toDomain(CreateHolidayRequest holidayRequest);

    Holiday toDomain(UpdateHolidayRequest holidayRequest);
}
