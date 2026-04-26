package com.uit.backend_cinema.modules.price_config.infrastructure.repository;

import com.uit.backend_cinema.modules.price_config.infrastructure.entity.HolidayJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface JpaHolidayRepository extends JpaRepository<HolidayJpaEntity, Long> {
    @Query("""
    SELECT (COUNT(h) > 0)
    FROM HolidayJpaEntity h
    WHERE (h.isRecurring = true AND MONTH(h.date) = :month AND DAY(h.date) = :day)
       OR (h.isRecurring = false AND h.date = :fullDate)
""")
    boolean isHoliday(
            @Param("month") int month,
            @Param("day") int day,
            @Param("fullDate") LocalDate fullDate
    );
}
