package com.uit.backend_cinema.modules.price_config.domain.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.price_config.domain.entity.PriceConfig;
import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;

public interface PriceConfigRepository {
    Optional<BigDecimal> findByDayType(DayType dayType);
    Optional<BigDecimal> findByTimeSlot(TimeSlot timeSlot);
    Optional<BigDecimal> findByMovieFormat(MovieFormat movieFormat);
    Optional<PriceConfig> findByConfigId(Long configId);

    List<PriceConfig> findAll();
    PriceConfig save(PriceConfig priceConfig);
}
