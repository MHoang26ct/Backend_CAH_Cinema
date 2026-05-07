package com.uit.backend_cinema.modules.price_config.infrastructure.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "price_configs")
@Getter
@Setter
public class PriceConfigJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long configId;
    @Enumerated(EnumType.STRING)
    private DayType dayType;
    @Enumerated(EnumType.STRING)
    private TimeSlot timeSlot;
    @Enumerated(EnumType.STRING)
    private MovieFormat movieFormat;
    private BigDecimal multiplier;
}
