package com.uit.backend_cinema.modules.price_config.infrastructure.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.price_config.domain.entity.Holiday;
import com.uit.backend_cinema.modules.price_config.domain.entity.PriceConfig;
import com.uit.backend_cinema.modules.price_config.infrastructure.entity.HolidayJpaEntity;
import com.uit.backend_cinema.modules.price_config.infrastructure.entity.PriceConfigJpaEntity;

@Mapper(componentModel = "spring")
public interface InfraMapper {
    Holiday toDomain(HolidayJpaEntity holidayJpaEntity);
    HolidayJpaEntity toJpaEntity(Holiday holiday);

    PriceConfig toDomain(PriceConfigJpaEntity priceConfigJpaEntity);
    PriceConfigJpaEntity toJpaEntity(PriceConfig priceConfig);
}
