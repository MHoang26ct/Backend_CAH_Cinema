package com.uit.backend_cinema.modules.price_config.infrastructure.persistence;

import com.uit.backend_cinema.modules.price_config.domain.entity.PriceConfig;
import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;
import com.uit.backend_cinema.modules.price_config.domain.repository.PriceConfigRepository;
import com.uit.backend_cinema.modules.price_config.infrastructure.entity.PriceConfigJpaEntity;
import com.uit.backend_cinema.modules.price_config.infrastructure.mapper.InfraMapper;
import com.uit.backend_cinema.modules.price_config.infrastructure.repository.JpaPriceConfigRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class PriceConfigRepositoryImpl implements PriceConfigRepository {
    private final JpaPriceConfigRepository jpaPriceConfigRepository;
    private final InfraMapper mapper;

    public PriceConfigRepositoryImpl(JpaPriceConfigRepository jpaPriceConfigRepository, InfraMapper mapper) {
        this.mapper = mapper;
        this.jpaPriceConfigRepository = jpaPriceConfigRepository;
    }

    @Override
    public Optional<BigDecimal> findByDayType(DayType dayType) {
        return jpaPriceConfigRepository.findByDayType(dayType).map(PriceConfigJpaEntity::getMultiplier);
    }

    @Override
    public Optional<BigDecimal> findByTimeSlot(TimeSlot timeSlot) {
        return jpaPriceConfigRepository.findByTimeSlot(timeSlot).map(PriceConfigJpaEntity::getMultiplier);
    }

    @Override
    public Optional<BigDecimal> findByMovieFormat(MovieFormat movieFormat) {
        return jpaPriceConfigRepository.findByMovieFormat(movieFormat).map(PriceConfigJpaEntity::getMultiplier);
    }

    @Override
    public Optional<PriceConfig> findByConfigId(Long configId) {
        return jpaPriceConfigRepository.findByConfigId(configId).map(mapper::toDomain);
    }

    @Override
    public List<PriceConfig> findAll() {
        return jpaPriceConfigRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PriceConfig save(PriceConfig priceConfig) {
        return mapper.toDomain(jpaPriceConfigRepository.save(mapper.toJpaEntity(priceConfig)));
    }
}
