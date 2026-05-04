package com.uit.backend_cinema.modules.showtime.infrastructure.entity;

import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.showtime.domain.entity.ShowtimeStatus;
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

    // MovieFormatConverter @autoApply = true tự xử lý "2D"/"3D"/"IMAX" ↔ enum
    @Column(name = "format", nullable = false)
    private MovieFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShowtimeStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;
}
