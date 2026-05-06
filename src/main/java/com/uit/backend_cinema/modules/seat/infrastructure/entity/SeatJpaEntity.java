package com.uit.backend_cinema.modules.seat.infrastructure.entity;

import com.uit.backend_cinema.modules.seat.domain.entity.SeatStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "seats")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
public class SeatJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_seq")
    @SequenceGenerator(name = "seat_seq", sequenceName = "seats_seq", allocationSize = 200)
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
