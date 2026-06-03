package com.uit.backend_cinema.unit_test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.uit.backend_cinema.modules.cinema.domain.entity.PendingRoomCleanup;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.repository.PendingRoomCleanupRepository;
import com.uit.backend_cinema.modules.cinema.domain.repository.RoomRepository;
import com.uit.backend_cinema.modules.cinema.domain.service.RoomCleanupScheduler;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoomCleanupSchedulerTest {

    @Test
    @DisplayName("cleanupLegacyRooms: Bỏ qua cleanup nếu vẫn còn suất chiếu trong tương lai")
    void cleanupLegacyRooms_activeShowtimes() {
        PendingRoomCleanupRepository pendingRoomCleanupRepository = mock(PendingRoomCleanupRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomCleanupScheduler scheduler = new RoomCleanupScheduler(pendingRoomCleanupRepository, showtimeRepository, seatRepository, roomRepository);

        PendingRoomCleanup cleanup = new PendingRoomCleanup(10L, 11L);
        when(pendingRoomCleanupRepository.findAllNotCleanedUp()).thenReturn(List.of(cleanup));
        
        // Mock max end time in the future
        when(showtimeRepository.findMaxEndTimeByRoomId(10L)).thenReturn(Optional.of(LocalDateTime.now().plusDays(1)));

        scheduler.cleanupLegacyRooms();

        // Ensure nothing was deleted or marked cleaned up
        verify(seatRepository, never()).softDeleteByRoomId(any());
        verify(roomRepository, never()).save(any());
        verify(pendingRoomCleanupRepository, never()).save(any());
        assertEquals(false, cleanup.getCleanedUp());
    }

    @Test
    @DisplayName("cleanupLegacyRooms: Thực hiện cleanup nếu tất cả suất chiếu đã kết thúc")
    void cleanupLegacyRooms_success() {
        PendingRoomCleanupRepository pendingRoomCleanupRepository = mock(PendingRoomCleanupRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        RoomCleanupScheduler scheduler = new RoomCleanupScheduler(pendingRoomCleanupRepository, showtimeRepository, seatRepository, roomRepository);

        PendingRoomCleanup cleanup = new PendingRoomCleanup(10L, 11L);
        when(pendingRoomCleanupRepository.findAllNotCleanedUp()).thenReturn(List.of(cleanup));
        
        // Mock max end time in the past
        when(showtimeRepository.findMaxEndTimeByRoomId(10L)).thenReturn(Optional.of(LocalDateTime.now().minusHours(1)));

        Room oldRoom = new Room();
        oldRoom.setRoomId(10L);
        oldRoom.setDeleted(false);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(oldRoom));

        scheduler.cleanupLegacyRooms();

        // Verify seats deleted
        verify(seatRepository).softDeleteByRoomId(10L);
        
        // Verify room soft deleted
        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertTrue(roomCaptor.getValue().isDeleted());

        // Verify cleanup marked done
        ArgumentCaptor<PendingRoomCleanup> cleanupCaptor = ArgumentCaptor.forClass(PendingRoomCleanup.class);
        verify(pendingRoomCleanupRepository).save(cleanupCaptor.capture());
        assertTrue(cleanupCaptor.getValue().getCleanedUp());
    }
}
