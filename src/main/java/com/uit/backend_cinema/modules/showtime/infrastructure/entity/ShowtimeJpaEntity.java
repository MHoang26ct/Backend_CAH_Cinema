package com.uit.backend_cinema.modules.showtime.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "showtimes")
@Getter @Setter
public class ShowtimeJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "showtime_id")
    private Long showtimeId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "format", nullable = false)
    private String format;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "status", nullable = false)
    private String status;
}
