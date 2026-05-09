package com.uit.backend_cinema.modules.seat.infrastructure.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import com.uit.backend_cinema.modules.seat.domain.entity.SeatStatus;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seats")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
public class SeatJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "seat_row", nullable = false)
    private BigDecimal seatRow;

    @Column(name = "seat_col", nullable = false)
    private BigDecimal seatCol;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seat_type_id", nullable = false)
    private SeatTypeJpaEntity seatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
}
