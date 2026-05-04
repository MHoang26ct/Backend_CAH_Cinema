package com.uit.backend_cinema.modules.seat.domain.service;

import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@Transactional(readOnly = true)
public class SeatService {
    private static final long LOCK_TTL_SECONDS = 600; // 10 phút

    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;

    public SeatService(SeatRepository seatRepository, SeatLockRepository seatLockRepository) {
        this.seatRepository = seatRepository;
        this.seatLockRepository = seatLockRepository;
    }

    // Lấy danh sách ghế theo phòng, kèm trạng thái lock từ Redis
    public List<Seat> getSeatsByShowtime(Long roomId, Long showtimeId) {
        List<Seat> seats = seatRepository.findByRoomId(roomId);

        seats.forEach(seat ->
            seat.setIsLocked(seatLockRepository.isLocked(showtimeId, seat.getSeatId()))
        );

        return seats;
    }

    // User chọn ghế → lock trong Redis 10 phút
    public boolean selectSeat(Long showtimeId, Long seatId, Long userId) {
        return seatLockRepository.lockSeat(showtimeId, seatId, userId, LOCK_TTL_SECONDS);
    }

    // Bỏ chọn ghế → unlock
    public void deselectSeat(Long showtimeId, Long seatId) {
        seatLockRepository.unlock(showtimeId, seatId);
    }
}
