package com.uit.backend_cinema.modules.seat.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatTypeRepository;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SeatService {
    private static final long PRE_LOCK_TTL_SECONDS = 300; // 5 phút
    private static final long CHECKOUT_LOCK_TTL_SECONDS = 900; // 15 phút

    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final TicketService ticketService;

    public SeatService(SeatRepository seatRepository, SeatLockRepository seatLockRepository,
            SeatTypeRepository seatTypeRepository, TicketService ticketService) {
        this.seatRepository = seatRepository;
        this.seatLockRepository = seatLockRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.ticketService = ticketService;
    }

    // Lấy danh sách ghế theo phòng, kèm trạng thái lock từ Redis
    public List<Seat> getSeatsByRoomId(Long roomId, Long showtimeId) {
        List<Seat> seats = seatRepository.findByRoomId(roomId);
        Set<Long> soldSeatIds = new java.util.HashSet<>(ticketService.findSoldSeatIdsByShowtimeId(showtimeId));
        seats.forEach(seat -> {
            boolean sold = soldSeatIds.contains(seat.getSeatId());
            boolean locked = seatLockRepository.isLocked(showtimeId, seat.getSeatId());
            seat.setIsSold(sold);
            seat.setIsLocked(locked);
            seat.setOccupancyStatus(resolveOccupancyStatus(sold, locked));
        });
        return seats;
    }

    // Bỏ chọn ghế → unlock (kiểm tra đúng chủ sở hữu)
    public void deselectSeat(Long showtimeId, Long seatId, Long userId) {
        String lockedBy = seatLockRepository.getLockedBy(showtimeId, seatId);
        if (lockedBy == null)
            return;
        if (!String.valueOf(userId).equals(lockedBy)) {
            throw new BusinessException("Bạn không có quyền bỏ chọn ghế này", ErrorCode.FORBIDDEN);
        }
        seatLockRepository.unlock(showtimeId, seatId);
    }

    @Transactional
    public boolean preLockSeats(Long showtimeId, List<Long> seatIds, Long roomId, Long userId) {
        List<Seat> seats = getValidatedSeatsForBooking(seatIds, roomId);
        ticketService.validateSeatsNotSold(showtimeId, seats.stream().map(Seat::getSeatId).toList());
        List<Long> lockedSeatIds = new ArrayList<>();
        for (Seat seat : seats) {
            boolean success = seatLockRepository.lockSeat(showtimeId, seat.getSeatId(), userId, PRE_LOCK_TTL_SECONDS);
            if (!success) {
                lockedSeatIds.forEach(lockedSeatId -> seatLockRepository.unlock(showtimeId, lockedSeatId));
                return false;
            }
            lockedSeatIds.add(seat.getSeatId());
        }
        return true;
    }

    @Transactional
    public List<Seat> promoteLocksForCheckout(Long showtimeId, List<Long> seatIds, Long roomId, Long userId) {
        List<Seat> seats = getValidatedSeatsForBooking(seatIds, roomId);
        ticketService.validateSeatsNotSold(showtimeId, seats.stream().map(Seat::getSeatId).toList());
        List<Long> promotedSeatIds = new ArrayList<>();
        for (Seat seat : seats) {
            boolean promoted = seatLockRepository.promoteLockIfOwner(
                    showtimeId,
                    seat.getSeatId(),
                    userId,
                    CHECKOUT_LOCK_TTL_SECONDS
            );
            if (!promoted) {
                promotedSeatIds.forEach(seatId -> seatLockRepository.unlock(showtimeId, seatId));
                throw new BusinessException("Một hoặc nhiều ghế không còn được giữ bởi bạn", ErrorCode.SEAT_ALREADY_BOOKED);
            }
            promotedSeatIds.add(seat.getSeatId());
        }
        return seats;
    }

    private String resolveOccupancyStatus(Boolean isSold, Boolean isLocked) {
        if (Boolean.TRUE.equals(isSold)) {
            return "SOLD";
        }
        if (Boolean.TRUE.equals(isLocked)) {
            return "LOCKED";
        }
        return "AVAILABLE";
    }

    @Transactional(readOnly = true)
    public void validateSeatsNotSold(Long showtimeId, List<Long> seatIds) {
        ticketService.validateSeatsNotSold(showtimeId, normalizeSeatIds(seatIds));
    }

    @Transactional
    public void releaseSeatLocksByOwner(Long showtimeId, List<Long> seatIds, Long userId) {
        List<Long> normalizedSeatIds = normalizeSeatIds(seatIds);
        for (Long seatId : normalizedSeatIds) {
            String lockedBy = seatLockRepository.getLockedBy(showtimeId, seatId);
            if (String.valueOf(userId).equals(lockedBy)) {
                seatLockRepository.unlock(showtimeId, seatId);
            }
        }
    }

    public List<Seat> getValidatedSeatsForBooking(List<Long> seatIds, Long roomId) {
        List<Long> normalizedSeatIds = normalizeSeatIds(seatIds);
        List<Seat> seats = seatRepository.findByIds(normalizedSeatIds);
        if (seats.size() != normalizedSeatIds.size()) {
            throw new BusinessException("Một hoặc nhiều ghế không tồn tại", ErrorCode.RESOURCE_NOT_FOUND);
        }
        for (Seat seat : seats) {
            if ("AISLE".equals(seat.getSeatType().getTypeName())) {
                throw new BusinessException("Không thể chọn lối đi", ErrorCode.VALIDATION_FAILED);
            }
            if (!seat.getRoomId().equals(roomId)) {
                throw new BusinessException("Ghế không thuộc phòng của suất chiếu này", ErrorCode.VALIDATION_FAILED);
            }
        }
        validateCoupleSeats(seats);
        return seats;
    }

    // Tạo sơ đồ ghế
    @Transactional
    public void createSeatMap(List<Seat> seatMap) {
        if (isValidSeatMap(seatMap)) {
            seatRepository.createSeatMap(seatMap);
        }
    }

    // Soft delete tất cả ghế của 1 phòng
    @Transactional
    public void deleteSeatsByRoomId(Long roomId) {
        if (!seatRepository.existsByRoomId(roomId))
            throw new BusinessException("Không tìm thấy phòng", ErrorCode.RESOURCE_NOT_FOUND);
        seatRepository.softDeleteByRoomId(roomId);
    }

    private boolean isValidSeatMap(List<Seat> seatMap) {
        for (int i = 1; i < seatMap.size(); i++) {
            if (seatMap.get(i).getRoomId().equals(seatMap.get(0).getRoomId())) {
                throw new BusinessException("Có 1 hoặc nhiều ghế không thuộc cùng 1 phòng",
                        ErrorCode.VALIDATION_FAILED);
            }
        }
        if (seatRepository.existsByRoomId(seatMap.get(0).getRoomId())) {
            throw new BusinessException("Đã tồn tại sơ đồ ghế cho phòng này", ErrorCode.VALIDATION_FAILED);
        }
        Set<Long> uniqueSeatTypeIds = seatMap.stream().map(seat -> seat.getSeatType().getSeatTypeId()).collect(Collectors.toSet());
        List<SeatType> seatTypes = seatTypeRepository.getSeatTypesByIds(uniqueSeatTypeIds.stream().toList());
        if (seatTypes.size() != uniqueSeatTypeIds.size()) {
            throw new BusinessException("Có 1 hoặc nhiều loại ghế không tồn tại", ErrorCode.VALIDATION_FAILED);
        }
        Map<Long, String> seatTypeMap = seatTypes.stream().collect(Collectors.toMap(SeatType::getSeatTypeId, SeatType::getTypeName));
        for (int i = 0; i < seatMap.size() - 1; i++) {
            Seat curr = seatMap.get(i);
            Seat next = seatMap.get(i + 1);
            if (seatTypeMap.get(curr.getSeatType().getSeatTypeId()).equals("COUPLE")) {
                if (curr.getSeatType().getSeatTypeId().equals(next.getSeatType().getSeatTypeId())) {
                    throw new BusinessException("Ghế đôi phải đi thành cặp", ErrorCode.VALIDATION_FAILED);
                }
                else if (curr.getSeatRow() != next.getSeatRow()) {
                    throw new BusinessException("Ghế đôi phải cùng hàng", ErrorCode.VALIDATION_FAILED);
                }
                else {
                    // Nếu cùng loại ghế, thì tăng i thêm 1 để skip qua
                    i++;
                }
            }
        }
        return true;
    }

    private List<Long> normalizeSeatIds(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new BusinessException("Danh sách ghế không được trống", ErrorCode.VALIDATION_FAILED);
        }
        List<Long> normalizedSeatIds = new ArrayList<>(new LinkedHashSet<>(seatIds));
        if (normalizedSeatIds.size() != seatIds.size()) {
            throw new BusinessException("Danh sách ghế có phần tử trùng lặp", ErrorCode.VALIDATION_FAILED);
        }
        return normalizedSeatIds;
    }

    private void validateCoupleSeats(List<Seat> seats) {
        Map<BigDecimal, Set<BigDecimal>> selectedCoupleSeatsByRow = seats.stream()
                .filter(seat -> "COUPLE".equals(seat.getSeatType().getTypeName()))
                .collect(Collectors.groupingBy(
                        Seat::getSeatRow,
                        Collectors.mapping(Seat::getSeatCol, Collectors.toSet())
                ));

        for (Map.Entry<BigDecimal, Set<BigDecimal>> entry : selectedCoupleSeatsByRow.entrySet()) {
            BigDecimal row = entry.getKey();
            Set<BigDecimal> cols = entry.getValue();
            for (BigDecimal col : cols) {
                boolean hasPairOnLeft = cols.stream().anyMatch(other ->
                        other.compareTo(col.subtract(BigDecimal.ONE)) == 0);
                boolean hasPairOnRight = cols.stream().anyMatch(other ->
                        other.compareTo(col.add(BigDecimal.ONE)) == 0);
                if (!hasPairOnLeft && !hasPairOnRight) {
                    throw new BusinessException(
                            "Ghế đôi ở hàng " + row + " cột " + col + " chưa đủ cặp",
                            ErrorCode.VALIDATION_FAILED
                    );
                }
            }
        }
    }
}
