package com.uit.backend_cinema.modules.price_config.infrastructure.repository;

import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;
import com.uit.backend_cinema.modules.price_config.infrastructure.entity.PriceConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPriceConfigRepository extends JpaRepository<PriceConfigJpaEntity, Long> {
    Optional<PriceConfigJpaEntity> findByDayType(DayType dayType);

    Optional<PriceConfigJpaEntity> findByTimeSlot(TimeSlot timeSlot);

    Optional<PriceConfigJpaEntity> findByMovieFormat(MovieFormat movieFormat);

    Optional<PriceConfigJpaEntity> findByConfigId(Long priceConfigId);
}
