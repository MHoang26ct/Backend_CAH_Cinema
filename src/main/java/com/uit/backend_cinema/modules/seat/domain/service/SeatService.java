package com.uit.backend_cinema.modules.seat.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SeatService {
    private static final long LOCK_TTL_SECONDS = 600; // 10 phút

    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;
    private final ShowtimeRepository showtimeRepository;

    public SeatService(SeatRepository seatRepository,
                       SeatLockRepository seatLockRepository,
                       ShowtimeRepository showtimeRepository) {
        this.seatRepository = seatRepository;
        this.seatLockRepository = seatLockRepository;
        this.showtimeRepository = showtimeRepository;
    }

    // Lấy danh sách ghế theo phòng, kèm trạng thái lock từ Redis
    // Validate roomId phải khớp với phòng của showtimeId
    public List<Seat> getSeatsByShowtime(Long roomId, Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy suất chiếu", ErrorCode.RESOURCE_NOT_FOUND));

        if (!showtime.getRoomId().equals(roomId)) {
            throw new BusinessException("roomId không thuộc suất chiếu này", ErrorCode.VALIDATION_FAILED);
        }

        List<Seat> seats = seatRepository.findByRoomId(roomId);
        seats.forEach(seat ->
            seat.setIsLocked(seatLockRepository.isLocked(showtimeId, seat.getSeatId()))
        );
        return seats;
    }

    // User chọn ghế → lock trong Redis 10 phút
    // Validate seatId phải thuộc phòng của showtimeId
    public boolean selectSeat(Long showtimeId, Long seatId, Long userId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy suất chiếu", ErrorCode.RESOURCE_NOT_FOUND));

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy ghế", ErrorCode.RESOURCE_NOT_FOUND));

        if (!seat.getRoomId().equals(showtime.getRoomId())) {
            throw new BusinessException("Ghế không thuộc phòng của suất chiếu này", ErrorCode.VALIDATION_FAILED);
        }

        return seatLockRepository.lockSeat(showtimeId, seatId, userId, LOCK_TTL_SECONDS);
    }

    // Bỏ chọn ghế → unlock (kiểm tra đúng chủ sở hữu)
    public void deselectSeat(Long showtimeId, Long seatId, Long userId) {
        String lockedBy = seatLockRepository.getLockedBy(showtimeId, seatId);
        if (lockedBy == null) return; // Ghế không bị lock, bỏ qua
        if (!String.valueOf(userId).equals(lockedBy)) {
            throw new BusinessException("Bạn không có quyền bỏ chọn ghế này", ErrorCode.FORBIDDEN);
        }
        seatLockRepository.unlock(showtimeId, seatId);
    }
}
