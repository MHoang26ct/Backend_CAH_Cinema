package com.uit.backend_cinema.modules.price_config.infrastructure.entity;

import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

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
    private MovieFormat movieFormat;
    private BigDecimal multiplier;
}
