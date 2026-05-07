package com.uit.backend_cinema.modules.price_config.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.price_config.domain.entity.Holiday;

public interface HolidayRepository {
    boolean isHoliday(LocalDate date);
    List<Holiday> findAll();
    Optional<Holiday> findById(Long id);
    Holiday save(Holiday holiday);
    void delete(Long id);
}
