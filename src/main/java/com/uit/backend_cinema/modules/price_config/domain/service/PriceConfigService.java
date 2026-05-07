package com.uit.backend_cinema.modules.price_config.domain.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.DateUtil;
import com.uit.backend_cinema.modules.price_config.domain.entity.PriceConfig;
import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;
import com.uit.backend_cinema.modules.price_config.domain.repository.HolidayRepository;
import com.uit.backend_cinema.modules.price_config.domain.repository.PriceConfigRepository;

@Service
@Transactional(readOnly = true)
public class PriceConfigService {
    private final PriceConfigRepository priceConfigRepository;
    private final HolidayRepository holidayRepository;

    public PriceConfigService(PriceConfigRepository priceConfigRepository, HolidayRepository holidayRepository) {
        this.priceConfigRepository = priceConfigRepository;
        this.holidayRepository = holidayRepository;
    }

    public List<PriceConfig> fetchAllPriceConfigs() {
        List<PriceConfig> configs = priceConfigRepository.findAll();
        if (configs.isEmpty()) {
            throw new BusinessException("Không có cấu hình giá nào trong hệ thống", ErrorCode.RESOURCE_NOT_FOUND);
        }
        return configs;
    }

    @Transactional
    public PriceConfig updatePriceConfig(PriceConfig priceConfig) {
        Long id = priceConfig.getConfigId();
        if (id == null) {
            throw new IllegalArgumentException("ID cấu hình giá không được null");
        }
        Optional<PriceConfig> existingPriceConfig = priceConfigRepository.findByConfigId(id);
        if (existingPriceConfig.isEmpty()) {
            throw new IllegalArgumentException("Cấu hình giá không tồn tại");
        }
        existingPriceConfig.get().setDayType(priceConfig.getDayType());
        existingPriceConfig.get().setTimeSlot(priceConfig.getTimeSlot());
        existingPriceConfig.get().setMovieFormat(priceConfig.getMovieFormat());
        existingPriceConfig.get().setMultiplier(priceConfig.getMultiplier());
        return priceConfigRepository.save(existingPriceConfig.get());
    }

    public BigDecimal getPriceMultiplier(LocalDateTime datetime, MovieFormat format) {
        if (datetime == null || format == null) {
            throw new IllegalArgumentException("Dữ liệu đầu vào không được null");
        }
        BigDecimal formatMultiplier = priceConfigRepository.findByMovieFormat(format)
                .orElse(BigDecimal.ONE);
        BigDecimal timeMultiplier;
        LocalDate date = datetime.toLocalDate();
        if (holidayRepository.isHoliday(date)) {
            timeMultiplier = priceConfigRepository.findByDayType(DayType.HOLIDAY)
                    .orElse(BigDecimal.valueOf(1.5));
        }
        else if (DateUtil.isWeekend(date)) {
            timeMultiplier = priceConfigRepository.findByDayType(DayType.WEEKEND)
                    .orElse(BigDecimal.valueOf(1.2));
        }
        else {
            int hour = datetime.getHour();
            TimeSlot slot = selectTimeSlot(hour);
            timeMultiplier = priceConfigRepository.findByTimeSlot(slot)
                    .orElse(BigDecimal.ONE);
        }

        return timeMultiplier.multiply(formatMultiplier);
    }

    private TimeSlot selectTimeSlot(int hour) {
        if (hour >= 6 && hour < 12) return TimeSlot.MORNING;
        if (hour >= 12 && hour < 18) return TimeSlot.AFTERNOON;
        return TimeSlot.EVENING;
    }
}
