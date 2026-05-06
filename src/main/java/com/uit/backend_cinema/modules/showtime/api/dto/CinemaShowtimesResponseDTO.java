package com.uit.backend_cinema.modules.showtime.api.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CinemaShowtimesResponseDTO {
    private MovieInfo movie;
    private List<ShowtimeInfo> showtimes;

    @Data
    @Builder
    public static class MovieInfo {
        private Long movieId;
        private String title;
        private String posterUrl;
        private String ageRating;
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
