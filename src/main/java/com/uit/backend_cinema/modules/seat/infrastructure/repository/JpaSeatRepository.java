package com.uit.backend_cinema.modules.seat.infrastructure.repository;

import com.uit.backend_cinema.modules.seat.infrastructure.entity.SeatJpaEntity;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaSeatRepository extends JpaRepository<SeatJpaEntity, Long> {

    // SQLRestriction trên entity đã tự filter is_deleted = false
    // Kết quả sort theo tọa độ tăng dần → đúng thứ tự hiển thị trên seat map
    List<SeatJpaEntity> findByRoomIdOrderBySeatRowAscSeatColAsc(Long roomId);
    List<SeatJpaEntity> findBySeatIdInOrderBySeatRowAscSeatColAsc(List<Long> seatIds);

    boolean existsByRoomId(Long roomId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SeatJpaEntity s set s.isDeleted = true where s.roomId = :roomId and s.isDeleted = false")
    void softDeleteByRoomId(@Param("roomId") Long roomId);
}
