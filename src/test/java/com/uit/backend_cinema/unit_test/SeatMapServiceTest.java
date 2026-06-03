package com.uit.backend_cinema.unit_test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.modules.cinema.domain.entity.PendingRoomCleanup;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.repository.PendingRoomCleanupRepository;
import com.uit.backend_cinema.modules.cinema.domain.repository.RoomRepository;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.service.SeatMapService;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeatMapServiceTest {

    @Test
    @DisplayName("replaceSeatMap: Lỗi nếu phòng đang có pending cleanup chưa hoàn tất")
    void replaceSeatMap_failPendingCleanup() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        SeatService seatService = mock(SeatService.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        PendingRoomCleanupRepository pendingRoomCleanupRepository = mock(PendingRoomCleanupRepository.class);
        SeatMapService seatMapService = new SeatMapService(roomRepository, seatService, showtimeRepository, pendingRoomCleanupRepository);

        Long oldRoomId = 1L;
        Room oldRoom = new Room();
        oldRoom.setRoomId(oldRoomId);
        
        when(roomRepository.findById(oldRoomId)).thenReturn(Optional.of(oldRoom));
        when(pendingRoomCleanupRepository.existsByOldRoomIdAndCleanedUpFalse(oldRoomId)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> seatMapService.replaceSeatMap(oldRoomId, List.of()));
        assertEquals("Phòng này đang trong quá trình thay thế sơ đồ ghế, vui lòng thử lại sau", ex.getMessage());
    }

    @Test
    @DisplayName("replaceSeatMap: Lỗi nếu phòng không tồn tại")
    void replaceSeatMap_failRoomNotFound() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        SeatService seatService = mock(SeatService.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        PendingRoomCleanupRepository pendingRoomCleanupRepository = mock(PendingRoomCleanupRepository.class);
        SeatMapService seatMapService = new SeatMapService(roomRepository, seatService, showtimeRepository, pendingRoomCleanupRepository);

        Long oldRoomId = 1L;
        when(roomRepository.findById(oldRoomId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> seatMapService.replaceSeatMap(oldRoomId, List.of()));
        assertEquals("Không tìm thấy phòng chiếu", ex.getMessage());
    }

    @Test
    @DisplayName("replaceSeatMap: Thành công tạo phòng mới, lưu sơ đồ ghế mới, migrate showtime và lưu pending cleanup")
    void replaceSeatMap_success() {
        RoomRepository roomRepository = mock(RoomRepository.class);
        SeatService seatService = mock(SeatService.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        PendingRoomCleanupRepository pendingRoomCleanupRepository = mock(PendingRoomCleanupRepository.class);
        SeatMapService seatMapService = new SeatMapService(roomRepository, seatService, showtimeRepository, pendingRoomCleanupRepository);

        Long oldRoomId = 10L;
        Room oldRoom = new Room();
        oldRoom.setRoomId(oldRoomId);
        oldRoom.setCinemaId(2L);
        oldRoom.setRoomName("Room A");

        when(roomRepository.findById(oldRoomId)).thenReturn(Optional.of(oldRoom));
        when(pendingRoomCleanupRepository.existsByOldRoomIdAndCleanedUpFalse(oldRoomId)).thenReturn(false);

        // Mock saved new room
        Room newRoom = new Room();
        newRoom.setRoomId(11L); // new room id
        newRoom.setCinemaId(oldRoom.getCinemaId());
        newRoom.setRoomName(oldRoom.getRoomName());
        when(roomRepository.save(any(Room.class))).thenReturn(newRoom);

        // Input seats
        Seat s1 = new Seat();
        s1.setSeatId(101L);
        Seat s2 = new Seat();
        s2.setSeatId(102L);
        List<Seat> newSeats = List.of(s1, s2);

        // Execute
        seatMapService.replaceSeatMap(oldRoomId, newSeats);

        // Verify save new room
        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        Room savedRoom = roomCaptor.getValue();
        assertEquals(2L, savedRoom.getCinemaId());
        assertEquals("Room A", savedRoom.getRoomName());

        // Verify seats roomId updated to newRoomId
        assertEquals(11L, s1.getRoomId());
        assertEquals(11L, s2.getRoomId());
        verify(seatService).createSeatMap(newSeats);

        // Verify showtime migration
        LocalDateTime cutoff = LocalDate.now().plusDays(8).atStartOfDay();
        verify(showtimeRepository).updateRoomIdForShowtimesAfterDate(eq(10L), eq(11L), eq(cutoff));

        // Verify pending cleanup record
        ArgumentCaptor<PendingRoomCleanup> cleanupCaptor = ArgumentCaptor.forClass(PendingRoomCleanup.class);
        verify(pendingRoomCleanupRepository).save(cleanupCaptor.capture());
        PendingRoomCleanup savedCleanup = cleanupCaptor.getValue();
        assertEquals(10L, savedCleanup.getOldRoomId());
        assertEquals(11L, savedCleanup.getNewRoomId());
        assertEquals(false, savedCleanup.getCleanedUp());
    }
}
