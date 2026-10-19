package com.uit.backend_cinema.modules.showtime.domain.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowtimeCancellationService {
    private final ShowtimeService showtimeService;
    private final BookingService bookingService;

    /** The schedule change and all refund obligations either commit together or not at all. */
    @Transactional
    public int cancelByRoom(Long roomId, LocalDate fromDate, LocalDate toDate, String reason) {
        List<Showtime> cancelled = showtimeService.cancelShowtimesByRoomBetweenDates(roomId, fromDate, toDate);
        for (Showtime showtime : cancelled) {
            bookingService.refundBookingsForCancelledShowtime(showtime.getShowtimeId(), reason);
        }
        return cancelled.size();
    }
}
