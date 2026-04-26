package com.uit.backend_cinema.modules.price_config.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")
@Getter
@Setter
public class HolidayJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long holidayId;
    private String name;

    @Column(name = "holiday_date")
    private LocalDate date;
    private Boolean isRecurring;
}
