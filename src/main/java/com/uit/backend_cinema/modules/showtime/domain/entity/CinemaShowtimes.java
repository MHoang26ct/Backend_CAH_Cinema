package com.uit.backend_cinema.modules.showtime.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CinemaShowtimes {
    private MovieInfo movie;
    private List<ShowtimeInfo> showtimes;

    @Data
    public static class MovieInfo {
        private Long movieId;
        private String title;
        private String posterUrl;
        private String ageRating;
    }

    @Data
    public static class ShowtimeInfo {
        private Long showtimeId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String format;
        private BigDecimal basePrice;
        private String status;
        private String roomName;
    }
}
