package com.uit.backend_cinema.modules.report.infrastructure.repository;

import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingJpaEntity;
import com.uit.backend_cinema.modules.report.infrastructure.dto.CinemaRevenueProjection;
import com.uit.backend_cinema.modules.report.infrastructure.dto.DailyRevenueProjection;
import com.uit.backend_cinema.modules.report.infrastructure.dto.MovieRevenueProjection;
import com.uit.backend_cinema.modules.report.infrastructure.dto.OverviewProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportReadRepository extends JpaRepository<BookingJpaEntity, Long> {

    @Query(value = """
            SELECT
                COALESCE(SUM(b.total_price), 0)                           AS totalRevenue,
                COALESCE(SUM(t_agg.ticket_sum), 0)                        AS ticketRevenue,
                COALESCE(SUM(fo_agg.food_sum), 0)                         AS foodRevenue,
                COALESCE(SUM(t_agg.ticket_count), 0)                      AS totalTicketsSold,
                COUNT(DISTINCT b.booking_id)                               AS totalBookingsPaid,
                COALESCE(SUM(b.discount_amount), 0)                        AS totalDiscount
            FROM bookings b
            LEFT JOIN (
                SELECT booking_id,
                       SUM(price)  AS ticket_sum,
                       COUNT(*)    AS ticket_count
                FROM tickets
                GROUP BY booking_id
            ) t_agg ON t_agg.booking_id = b.booking_id
            LEFT JOIN (
                SELECT booking_id,
                       SUM(total_price) AS food_sum
                FROM food_orders
                GROUP BY booking_id
            ) fo_agg ON fo_agg.booking_id = b.booking_id
            WHERE b.status IN ('PAID', 'CHECKED_IN')
              AND b.created_at >= :from
              AND b.created_at < :to
            """,
            nativeQuery = true)
    Optional<OverviewProjection> findOverview(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT
                CAST(DATE(b.created_at) AS date)          AS reportDate,
                COALESCE(SUM(b.total_price), 0)           AS revenue,
                COUNT(DISTINCT b.booking_id)               AS bookingCount,
                COALESCE(SUM(t_agg.ticket_count), 0)      AS ticketCount
            FROM bookings b
            LEFT JOIN (
                SELECT booking_id, COUNT(*) AS ticket_count
                FROM tickets
                GROUP BY booking_id
            ) t_agg ON t_agg.booking_id = b.booking_id
            WHERE b.status IN ('PAID', 'CHECKED_IN')
              AND b.created_at >= :from
              AND b.created_at < :to
            GROUP BY DATE(b.created_at)
            ORDER BY DATE(b.created_at)
            """,
            nativeQuery = true)
    List<DailyRevenueProjection> findDailyRevenue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT
                m.movie_id                             AS movieId,
                m.title                                AS movieTitle,
                COALESCE(SUM(tk.price), 0)             AS ticketRevenue,
                COUNT(tk.ticket_id)                    AS ticketsSold,
                COUNT(DISTINCT b.booking_id)            AS bookingCount
            FROM tickets tk
            JOIN bookings b       ON b.booking_id  = tk.booking_id
            JOIN showtimes s      ON s.showtime_id = tk.showtime_id
            JOIN movies m         ON m.movie_id    = s.movie_id
            WHERE b.status IN ('PAID', 'CHECKED_IN')
              AND b.created_at >= :from
              AND b.created_at < :to
            GROUP BY m.movie_id, m.title
            ORDER BY ticketRevenue DESC
            """,
            nativeQuery = true)
    List<MovieRevenueProjection> findRevenueByMovie(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT
                c.cinema_id                            AS cinemaId,
                c.name                                 AS cinemaName,
                COALESCE(SUM(tk.price), 0)             AS ticketRevenue,
                COUNT(tk.ticket_id)                    AS ticketsSold,
                COUNT(DISTINCT b.booking_id)            AS bookingCount
            FROM tickets tk
            JOIN bookings b       ON b.booking_id  = tk.booking_id
            JOIN showtimes s      ON s.showtime_id = tk.showtime_id
            JOIN rooms r          ON r.room_id     = s.room_id
            JOIN cinemas c        ON c.cinema_id   = r.cinema_id
            WHERE b.status IN ('PAID', 'CHECKED_IN')
              AND b.created_at >= :from
              AND b.created_at < :to
            GROUP BY c.cinema_id, c.name
            ORDER BY ticketRevenue DESC
            """,
            nativeQuery = true)

    List<CinemaRevenueProjection> findRevenueByCinema(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
