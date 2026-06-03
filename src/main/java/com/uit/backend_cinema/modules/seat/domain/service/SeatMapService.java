package com.uit.backend_cinema.modules.seat.domain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.cinema.domain.entity.PendingRoomCleanup;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.repository.PendingRoomCleanupRepository;
import com.uit.backend_cinema.modules.cinema.domain.repository.RoomRepository;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;

/**
 * Orchestration service xử lý việc thay thế sơ đồ ghế (Room Cloning).
 * <p>
 * Quy trình:
 * 1. Validate room cũ tồn tại, chưa bị thay thế
 * 2. Tạo room mới cùng tên / cinema
 * 3. Tạo seat map mới cho room mới
 * 4. Migrate toàn bộ showtime > 7 ngày từ room cũ → room mới
 * 5. Tạo record pending_room_cleanups để scheduler dọn dẹp sau
 */
@Service
public class SeatMapService {
    private static final int ADVANCE_BOOKING_LIMIT_DAYS = 7;

    private final RoomRepository roomRepository;
    private final SeatService seatService;
    private final ShowtimeRepository showtimeRepository;
    private final PendingRoomCleanupRepository pendingRoomCleanupRepository;

    public SeatMapService(RoomRepository roomRepository,
                          SeatService seatService,
                          ShowtimeRepository showtimeRepository,
                          PendingRoomCleanupRepository pendingRoomCleanupRepository) {
        this.roomRepository = roomRepository;
        this.seatService = seatService;
        this.showtimeRepository = showtimeRepository;
        this.pendingRoomCleanupRepository = pendingRoomCleanupRepository;
    }

    /**
     * Thay thế sơ đồ ghế của phòng {@code oldRoomId}.
     * <ul>
     *   <li>Showtime ≤ 7 ngày: giữ nguyên (không migrate) — có thể đã có booking</li>
     *   <li>Showtime > 7 ngày: migrate sang room mới (chưa ai đặt vé)</li>
     * </ul>
     *
     * @param oldRoomId ID phòng cần thay thế sơ đồ ghế
     * @param newSeats  Danh sách ghế mới (roomId trong mỗi Seat sẽ bị override thành newRoom.roomId)
     */
    @Transactional
    public void replaceSeatMap(Long oldRoomId, List<Seat> newSeats) {
        // 1. Validate room cũ tồn tại
        Room oldRoom = roomRepository.findById(oldRoomId)
                .orElseThrow(() -> new BusinessException(
                        "Không tìm thấy phòng chiếu", ErrorCode.RESOURCE_NOT_FOUND));

        // 2. Validate chưa có pending cleanup chưa hoàn tất
        if (pendingRoomCleanupRepository.existsByOldRoomIdAndCleanedUpFalse(oldRoomId)) {
            throw new BusinessException(
                    "Phòng này đang trong quá trình thay thế sơ đồ ghế, vui lòng thử lại sau",
                    ErrorCode.VALIDATION_FAILED);
        }

        // 3. Tạo room mới cùng tên và cùng rạp
        Room newRoom = new Room();
        newRoom.setCinemaId(oldRoom.getCinemaId());
        newRoom.setRoomName(oldRoom.getRoomName());
        newRoom = roomRepository.save(newRoom);

        // 4. Tạo seat map mới (gán roomId mới)
        final Long newRoomId = newRoom.getRoomId();
        newSeats.forEach(s -> s.setRoomId(newRoomId));
        seatService.createSeatMap(newSeats);

        // 5. Migrate showtime nằm ngoài giới hạn 7 ngày (chưa có booking) sang room mới
        //    Cutoff = đầu ngày (D+8) → mọi showtime từ D+8 trở đi đều migrate
        LocalDateTime cutoff = LocalDate.now()
                .plusDays(ADVANCE_BOOKING_LIMIT_DAYS + 1)
                .atStartOfDay();
        int migratedCount = showtimeRepository.updateRoomIdForShowtimesAfterDate(
                oldRoomId, newRoomId, cutoff);

        // 6. Tạo record cleanup để scheduler dọn dẹp room cũ sau khi showtime kết thúc
        pendingRoomCleanupRepository.save(new PendingRoomCleanup(oldRoomId, newRoomId));
    }
}
