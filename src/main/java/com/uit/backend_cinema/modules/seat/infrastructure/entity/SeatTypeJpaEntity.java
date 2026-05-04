package com.uit.backend_cinema.modules.seat.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

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
