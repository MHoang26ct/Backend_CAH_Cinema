package com.uit.backend_cinema.modules.price_config.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.price_config.domain.entity.Holiday;
import com.uit.backend_cinema.modules.price_config.domain.repository.HolidayRepository;

@Service
@Transactional(readOnly = true)
public class HolidayService {
    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public List<Holiday> fetchAllHolidays() {
        List<Holiday> holidays = holidayRepository.findAll();
        if (holidays.isEmpty()) {
            throw new BusinessException("Không có ngày lễ nào trong hệ thống", ErrorCode.RESOURCE_NOT_FOUND);
        }
        return holidays;
    }
    @Transactional
    public Holiday createHoliday(Holiday holiday) {
        boolean isHoliday = holidayRepository.isHoliday(holiday.getDate());
        if (isHoliday) {
            throw new IllegalArgumentException("Ngày lễ đã tồn tại");
        }
        return holidayRepository.save(holiday);
    }

    @Transactional
    public Holiday updateHoliday(Holiday holiday) {
        Optional<Holiday> existingHoliday = holidayRepository.findById(holiday.getHolidayId());
        if (existingHoliday.isEmpty()) {
            throw new IllegalArgumentException("Ngày lễ không tồn tại");
        }
        existingHoliday.get().setDate(holiday.getDate());
        existingHoliday.get().setIsRecurring(holiday.getIsRecurring());
        existingHoliday.get().setName(holiday.getName());
        return holidayRepository.save(existingHoliday.get());
    }

    @Transactional
    public void deleteHoliday(Long id) {
        Optional<Holiday> holiday = holidayRepository.findById(id);
        if (holiday.isEmpty()) {
            throw new IllegalArgumentException("Ngày lễ không tồn tại");
        }
        holidayRepository.delete(id);
    }
}
