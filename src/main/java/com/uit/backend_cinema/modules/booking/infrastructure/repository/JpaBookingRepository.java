package com.uit.backend_cinema.modules.booking.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uit.backend_cinema.modules.auth.infrastructure.projection.BookingInvoiceRow;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingJpaEntity;

public interface JpaBookingRepository extends JpaRepository<BookingJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingJpaEntity b where b.bookingId = :bookingId")
    Optional<BookingJpaEntity> findByIdForUpdate(@Param("bookingId") Long bookingId);

    List<BookingJpaEntity> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime threshold);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update bookings
            set status = 'EXPIRED', updated_at = :now, version = version + 1
            where booking_id = :bookingId
              and status = 'PENDING'
              and expires_at < :now
              and is_deleted = false
            """, nativeQuery = true)
    int markExpiredIfPendingAndExpired(@Param("bookingId") Long bookingId, @Param("now") LocalDateTime now);

    /**
     * Lấy 5 booking gần nhất (PAID / CHECKED_IN) của user, kèm đầy đủ thông tin
     * showtime, movie, cinema, room, ghế, thức ăn.
     *
     * Mỗi row = 1 ghế × 1 food item (null khi không có food).
     * UseCase sẽ group + assemble thành List&lt;FullInvoiceDTO&gt;.
     */
    @Query(value = """
            SELECT
                b.booking_id            AS bookingId,
                b.status                AS bookingStatus,
                b.payment_method        AS paymentMethod,
                b.discount_amount       AS discountAmount,
                b.total_price           AS totalPrice,
                b.created_at            AS bookingCreatedAt,

                v.code                  AS voucherCode,

                st.showtime_id          AS showtimeId,
                st.format               AS movieFormat,
                st.start_time           AS startTime,
                st.end_time             AS endTime,

                m.movie_id              AS movieId,
                m.title                 AS movieTitle,
                m.poster_url            AS moviePosterUrl,

                ci.name                 AS cinemaName,
                r.room_name             AS roomName,

                s.seat_id               AS seatId,
                s.seat_row              AS seatRow,
                s.seat_col              AS seatCol,
                sty.type_name           AS seatType,
                tk.price                AS ticketPrice,

                fi.food_id              AS foodId,
                fi.name                 AS foodName,
                fi.image_url            AS foodImageUrl,
                fi.category             AS foodCategory,
                foi.quantity            AS foodQuantity,
                foi.price               AS foodUnitPrice,
                fo.total_price          AS foodTotalPrice

            FROM (
                SELECT * FROM bookings
                WHERE user_id    = :userId
                  AND status     IN ('PAID', 'CHECKED_IN')
                  AND is_deleted = FALSE
                ORDER BY created_at DESC
                LIMIT 5
            ) b

            LEFT JOIN vouchers        v   ON v.voucher_id    = b.voucher_id
            JOIN  showtimes           st  ON st.showtime_id  = b.showtime_id
            JOIN  movies              m   ON m.movie_id      = st.movie_id
            JOIN  rooms               r   ON r.room_id       = st.room_id
            JOIN  cinemas             ci  ON ci.cinema_id    = r.cinema_id
            JOIN  tickets             tk  ON tk.booking_id   = b.booking_id
            JOIN  seats               s   ON s.seat_id       = tk.seat_id
            JOIN  seat_types          sty ON sty.seat_type_id = s.seat_type_id
            LEFT JOIN food_orders     fo  ON fo.booking_id   = b.booking_id
            LEFT JOIN food_order_items foi ON foi.food_order_id = fo.food_order_id
            LEFT JOIN foods           fi  ON fi.food_id      = foi.food_id

            ORDER BY b.created_at DESC, b.booking_id, s.seat_row, s.seat_col
            """, nativeQuery = true)
    List<BookingInvoiceRow> findTop5FullInvoicesByUserId(@Param("userId") Long userId);
}
