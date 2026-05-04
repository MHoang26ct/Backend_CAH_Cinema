package com.uit.backend_cinema.modules.showtime.infrastructure.persistence;

import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class ShowtimeEnricher {

    @PersistenceContext
    private EntityManager em;

    public Showtime enrich(Showtime showtime) {
        // Lấy room + cinema info
        Object[] roomInfo = (Object[]) em.createNativeQuery("""
            SELECT r.room_name, r.cinema_id, c.name
            FROM rooms r JOIN cinemas c ON r.cinema_id = c.cinema_id
            WHERE r.room_id = :roomId
        """)
        .setParameter("roomId", showtime.getRoomId())
        .getSingleResult();

        showtime.setRoomName((String) roomInfo[0]);
        showtime.setCinemaId(((Number) roomInfo[1]).longValue());
        showtime.setCinemaName((String) roomInfo[2]);

        // Lấy movie info
        Object[] movieInfo = (Object[]) em.createNativeQuery("""
            SELECT title, poster_url, duration
            FROM movies WHERE movie_id = :movieId
        """)
        .setParameter("movieId", showtime.getMovieId())
        .getSingleResult();

        showtime.setMovieTitle((String) movieInfo[0]);
        showtime.setMoviePosterUrl((String) movieInfo[1]);
        showtime.setMovieDuration(((Number) movieInfo[2]).intValue());

        return showtime;
    }
}
