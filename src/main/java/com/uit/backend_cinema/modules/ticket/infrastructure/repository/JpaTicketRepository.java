package com.uit.backend_cinema.modules.ticket.infrastructure.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.ticket.infrastructure.entity.TicketJpaEntity;

public interface JpaTicketRepository extends JpaRepository<TicketJpaEntity, Long> {
    boolean existsByBookingIdAndSeatId(Long bookingId, Long seatId);

    List<TicketJpaEntity> findByBookingId(Long bookingId);

    boolean existsByShowtimeIdAndSeatIdIn(Long showtimeId, Collection<Long> seatIds);

    @Query("""
            select t.seatId
            from TicketJpaEntity t, BookingJpaEntity b
            where t.bookingId = b.bookingId
              and t.showtimeId = :showtimeId
              and b.status in :paidStatuses
            """)
    List<Long> findSoldSeatIdsByShowtimeId(@Param("showtimeId") Long showtimeId,
                                            @Param("paidStatuses") Collection<BookingStatus> paidStatuses);
}
