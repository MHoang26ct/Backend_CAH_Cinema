package com.uit.backend_cinema.modules.seat.infrastructure.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seat_types")
@Getter @Setter
public class SeatTypeJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_type_id")
    private Long seatTypeId;

    @Column(name = "type_name", nullable = false)
    private String typeName;

    @Column(name = "price_multiplier", nullable = false)
    private BigDecimal priceMultiplier;
}
