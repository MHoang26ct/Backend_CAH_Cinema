package com.uit.backend_cinema.modules.cinema.domain.repository;

import java.util.List;

import com.uit.backend_cinema.modules.cinema.domain.entity.PendingRoomCleanup;

public interface PendingRoomCleanupRepository {
    PendingRoomCleanup save(PendingRoomCleanup cleanup);

    /** Lấy tất cả record chưa được cleanup (dùng cho scheduler) */
    List<PendingRoomCleanup> findAllNotCleanedUp();

    /** Kiểm tra room cũ đã có pending cleanup chưa (dùng trước khi thay thế sơ đồ ghế) */
    boolean existsByOldRoomIdAndCleanedUpFalse(Long oldRoomId);
}
