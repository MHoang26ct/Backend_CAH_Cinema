package com.uit.backend_cinema.modules.price_config.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.price_config.domain.entity.Holiday;
import com.uit.backend_cinema.modules.price_config.domain.repository.HolidayRepository;
import com.uit.backend_cinema.modules.price_config.infrastructure.mapper.InfraMapper;
import com.uit.backend_cinema.modules.price_config.infrastructure.repository.JpaHolidayRepository;

@Repository
public class HolidayRepositoryImpl implements HolidayRepository {
    private final JpaHolidayRepository jpaHolidayRepository;
    private final InfraMapper mapper;

    public HolidayRepositoryImpl(JpaHolidayRepository jpaHolidayRepository, InfraMapper mapper) {
        this.mapper = mapper;
        this.jpaHolidayRepository = jpaHolidayRepository;
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        return jpaHolidayRepository.isHoliday(date.getMonthValue(), date.getDayOfMonth(), date);
    }

    @Override
    public List<Holiday> findAll() {
        return jpaHolidayRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
    @Override
    public Optional<Holiday> findById(Long id) {
        return jpaHolidayRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Holiday save(Holiday holiday) {
        return mapper.toDomain(jpaHolidayRepository.save(mapper.toJpaEntity(holiday)));
    }

    @Override
    public void delete(Long id) {
        jpaHolidayRepository.deleteById(id);
    }
}
