package com.uit.backend_cinema.modules.cinema.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.modules.cinema.domain.entity.PendingRoomCleanup;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.repository.PendingRoomCleanupRepository;
import com.uit.backend_cinema.modules.cinema.domain.repository.RoomRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;

/**
 * Scheduler tự động dọn dẹp phòng cũ sau khi tất cả suất chiếu của nó đã kết thúc.
 * <p>
 * Chạy theo chu kỳ được cấu hình bởi {@code room.cleanup.fixed-delay-ms} (mặc định 24h).
 * Với mỗi record trong {@code pending_room_cleanups} chưa được cleanup:
 * <ul>
 *   <li>Tìm thời điểm kết thúc muộn nhất của showtime trong phòng cũ</li>
 *   <li>Nếu không còn showtime nào hoặc showtime đã kết thúc → soft-delete ghế + phòng</li>
 * </ul>
 */
@Component
public class RoomCleanupScheduler {

    private final PendingRoomCleanupRepository pendingRoomCleanupRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;

    public RoomCleanupScheduler(PendingRoomCleanupRepository pendingRoomCleanupRepository,
                                ShowtimeRepository showtimeRepository,
                                SeatRepository seatRepository,
                                RoomRepository roomRepository) {
        this.pendingRoomCleanupRepository = pendingRoomCleanupRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.roomRepository = roomRepository;
    }

    @Scheduled(fixedDelayString = "${room.cleanup.fixed-delay-ms:86400000}")
    @Transactional
    public void cleanupLegacyRooms() {
        List<PendingRoomCleanup> pending = pendingRoomCleanupRepository.findAllNotCleanedUp();
        LocalDateTime now = LocalDateTime.now();

        for (PendingRoomCleanup cleanup : pending) {
            tryCleanup(cleanup, now);
        }
    }

    private void tryCleanup(PendingRoomCleanup cleanup, LocalDateTime now) {
        Optional<LocalDateTime> lastEndTime =
                showtimeRepository.findMaxEndTimeByRoomId(cleanup.getOldRoomId());

        // Chỉ cleanup nếu không còn showtime nào chưa kết thúc
        if (lastEndTime.isPresent() && lastEndTime.get().isAfter(now)) {
            return; // Còn showtime đang chạy, chưa cleanup
        }

        // Soft-delete ghế
        seatRepository.softDeleteByRoomId(cleanup.getOldRoomId());

        // Soft-delete phòng
        roomRepository.findById(cleanup.getOldRoomId()).ifPresent(room -> {
            room.setDeleted(true);
            roomRepository.save(room);
        });

        // Đánh dấu đã cleanup
        cleanup.setCleanedUp(true);
        cleanup.setCleanedUpAt(now);
        pendingRoomCleanupRepository.save(cleanup);
    }
}
