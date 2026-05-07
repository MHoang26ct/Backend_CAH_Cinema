package com.uit.backend_cinema.modules.showtime.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MovieShowtimesResponseDTO {
    private MovieInfo movie;
    private List<CinemaShowtimes> cinemas;

    @Data
    @Builder
    public static class MovieInfo {
        private Long movieId;
        private String title;
        private String description;
    }

    @Data
    @Builder
    public static class CinemaShowtimes {
        private Long cinemaId;
        private String cinemaName;
        private String address;
        private List<ShowtimeInfo> showtimes;
    }

    @Data
    @Builder
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
