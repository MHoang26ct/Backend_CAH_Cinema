package com.uit.backend_cinema.modules.ticket.domain.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.CheckinCodeUtil;
import com.uit.backend_cinema.modules.seat.api.util.SeatLabelUtil;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.ticket.api.dto.TicketItemResponseDTO;
import com.uit.backend_cinema.modules.ticket.domain.entity.PendingTicketItem;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.repository.PendingTicketItemRepository;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final PendingTicketItemRepository pendingTicketItemRepository;
    private final SeatRepository seatRepository;

    public TicketService(TicketRepository ticketRepository,
                         PendingTicketItemRepository pendingTicketItemRepository) {
        this(ticketRepository, pendingTicketItemRepository, null);
    }

    @Autowired
    public TicketService(TicketRepository ticketRepository,
                         PendingTicketItemRepository pendingTicketItemRepository,
                         SeatRepository seatRepository) {
        this.ticketRepository = ticketRepository;
        this.pendingTicketItemRepository = pendingTicketItemRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public List<PendingTicketItem> createDraftItems(Long bookingId,
                                                    List<Seat> seats,
                                                    Showtime showtime,
                                                    BigDecimal showtimeMultiplier) {
        List<PendingTicketItem> pendingTicketItems = seats.stream().map(seat -> {
            PendingTicketItem item = new PendingTicketItem();
            item.setBookingId(bookingId);
            item.setSeatId(seat.getSeatId());
            item.setUnitPrice(seatPrice(seat, showtime, showtimeMultiplier));
            item.setIsDeleted(false);
            return item;
        }).toList();
        return pendingTicketItemRepository.saveAll(pendingTicketItems);
    }

    @Transactional
    public List<Ticket> finalizeTicketsForPaidBooking(Long bookingId, Long showtimeId) {
        List<PendingTicketItem> pendingItems = pendingTicketItemRepository.findAllActiveByBookingId(bookingId);
        List<Ticket> tickets = new ArrayList<>();
        for (PendingTicketItem item : pendingItems) {
            Ticket ticket = new Ticket();
            ticket.setBookingId(item.getBookingId());
            ticket.setShowtimeId(showtimeId);
            ticket.setSeatId(item.getSeatId());
            ticket.setPrice(item.getUnitPrice());
            ticket.setCheckinCode(generateUniqueCheckinCode());
            tickets.add(ticket);
        }

        if (tickets.isEmpty()) {
            tickets = findAllByBookingId(bookingId);
            if (tickets.isEmpty()) {
                throw new BusinessException("Booking không có vé nháp để tạo vé", ErrorCode.RESOURCE_NOT_FOUND);
            }
            ensureCheckinCodes(tickets);
            return tickets;
        }

        createTicketIfAbsent(tickets);
        List<Ticket> savedTickets = findAllByBookingId(bookingId);
        ensureCheckinCodes(savedTickets);
        return savedTickets;
    }

    public String generateUniqueCheckinCode() {
        for (int i = 0; i < 10; i++) {
            String code = CheckinCodeUtil.generateCode();
            if (!ticketRepository.existsByCheckinCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không thể tạo mã vé duy nhất");
    }

    private void ensureCheckinCodes(List<Ticket> tickets) {
        for (Ticket ticket : tickets) {
            if (ticket.getCheckinCode() == null || ticket.getCheckinCode().isBlank()) {
                ticket.setCheckinCode(generateUniqueCheckinCode());
                ticketRepository.save(ticket);
            }
        }
    }

    @Transactional
    public List<TicketItemResponseDTO> getTicketDetailsByBookingId(Long bookingId) {
        List<Ticket> tickets = findAllByBookingId(bookingId);
        if (tickets == null || tickets.isEmpty()) {
            return Collections.emptyList();
        }
        ensureCheckinCodes(tickets);
        Map<Long, Seat> seatMap = Collections.emptyMap();
        if (seatRepository != null) {
            List<Seat> seats = seatRepository.findByIds(tickets.stream().map(Ticket::getSeatId).toList());
            if (seats != null) {
                seatMap = seats.stream().collect(Collectors.toMap(Seat::getSeatId, s -> s, (s1, s2) -> s1));
            }
        }
        final Map<Long, Seat> finalSeatMap = seatMap;
        return tickets.stream().map(t -> {
            Seat seat = finalSeatMap.get(t.getSeatId());
            String seatName = formatSeatName(seat);
            return TicketItemResponseDTO.builder()
                    .ticketId(t.getTicketId())
                    .bookingId(t.getBookingId())
                    .showtimeId(t.getShowtimeId())
                    .seatId(t.getSeatId())
                    .seatName(seatName)
                    .price(t.getPrice())
                    .checkinCode(t.getCheckinCode())
                    .qrCode(t.getCheckinCode())
                    .status(t.getStatus())
                    .isCheckedIn(t.getIsCheckedIn())
                    .build();
        }).toList();
    }

    private String formatSeatName(Seat seat) {
        if (seat == null) return "N/A";
        String row = SeatLabelUtil.toRowLabel(seat.getSeatRow());
        String col = SeatLabelUtil.toColLabel(seat.getSeatCol());
        if (row != null && col != null) {
            return row + col;
        }
        if (seat.getSeatRow() != null && seat.getSeatCol() != null) {
            char rowLetter = (char) ('A' + seat.getSeatRow().intValue() - 1);
            return rowLetter + String.valueOf(seat.getSeatCol().intValue());
        }
        return "N/A";
    }

    @Transactional
    public void expireDraftItems(Long bookingId) {
        pendingTicketItemRepository.softDeleteByBookingId(bookingId);
    }

    @Transactional
    public void purgeSoftDeletedDraftItems(LocalDateTime threshold) {
        pendingTicketItemRepository.hardDeleteSoftDeletedBefore(threshold);
    }

    @Transactional(readOnly = true)
    public List<Long> findActiveDraftSeatIds(Long bookingId) {
        return pendingTicketItemRepository.findAllActiveByBookingId(bookingId)
                .stream()
                .map(PendingTicketItem::getSeatId)
                .toList();
    }

    @Transactional
    public void createTicketIfAbsent(List<Ticket> tickets) {
        if (!validateTickets(tickets)) {
            return;
        }
        try {
            ticketRepository.saveAll(tickets);
        } catch (DataIntegrityViolationException ex) {
            if (hasSameExistingTickets(tickets)) {
                return;
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<Ticket> findAllByBookingId(Long bookingId) {
        return ticketRepository.findAllByBookingId(bookingId);
    }

    @Transactional
    public void cancelTicketsForBooking(Long bookingId) {
        List<Ticket> tickets = ticketRepository.findAllByBookingId(bookingId);
        for (Ticket ticket : tickets) {
            ticket.setStatus(com.uit.backend_cinema.modules.ticket.domain.entity.TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        }
    }

    @Transactional(readOnly = true)
    public boolean hasAnyCheckedInTicket(Long bookingId) {
        return ticketRepository.findAllByBookingId(bookingId)
                .stream()
                .anyMatch(t -> Boolean.TRUE.equals(t.getIsCheckedIn()));
    }

    private BigDecimal seatPrice(Seat seat, Showtime showtime, BigDecimal showtimeMultiplier) {
        return seat.getSeatType().getPriceMultiplier()
                .multiply(showtime.getBasePrice())
                .multiply(showtimeMultiplier);
    }

    private boolean validateTickets(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return false;
        }
        tickets.forEach(ticket -> {
            if (ticket.getBookingId() == null || ticket.getShowtimeId() == null || ticket.getSeatId() == null) {
                throw new BusinessException("BookingId, ShowtimeId và SeatId không được null", ErrorCode.VALIDATION_FAILED);
            }
        });
        Long bookingId = tickets.get(0).getBookingId();
        if (!tickets.stream().allMatch(ticket -> ticket.getBookingId().equals(bookingId))) {
            throw new BusinessException("Tất cả các vé phải có cùng một bookingId", ErrorCode.VALIDATION_FAILED);
        }
        Long showtimeId = tickets.get(0).getShowtimeId();
        if (!tickets.stream().allMatch(ticket -> ticket.getShowtimeId().equals(showtimeId))) {
            throw new BusinessException("Tất cả các vé phải có cùng một showtimeId", ErrorCode.VALIDATION_FAILED);
        }
        if (tickets.stream().map(Ticket::getSeatId).distinct().count() != tickets.size()) {
            throw new BusinessException("Danh sách vé có seatId trùng lặp", ErrorCode.VALIDATION_FAILED);
        }
        if (hasSameExistingTickets(tickets)) {
            return false;
        }
        if (!ticketRepository.findAllByBookingId(bookingId).isEmpty()) {
            throw new BusinessException("Booking đã có vé, không thể tạo thêm", ErrorCode.VALIDATION_FAILED);
        }
        return true;
    }

    private boolean hasSameExistingTickets(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return false;
        }
        Long bookingId = tickets.get(0).getBookingId();
        List<Ticket> existingTickets = ticketRepository.findAllByBookingId(bookingId);
        if (existingTickets.size() != tickets.size()) {
            return false;
        }
        return existingTickets.stream().map(Ticket::getSeatId).collect(java.util.stream.Collectors.toSet())
                .equals(tickets.stream().map(Ticket::getSeatId).collect(java.util.stream.Collectors.toSet()));
    }

    @Transactional(readOnly = true)
    public List<Long> findSoldSeatIdsByShowtimeId(Long showtimeId) {
        return ticketRepository.findSoldSeatIdsByShowtimeId(showtimeId);
    }

    @Transactional(readOnly = true)
    public void validateSeatsNotSold(Long showtimeId, List<Long> seatIds) {
        if (ticketRepository.existsSoldSeatByShowtimeIdAndSeatIds(showtimeId, seatIds)) {
            throw new BusinessException("Ghế đã được bán cho suất chiếu này", ErrorCode.SEAT_ALREADY_BOOKED);
        }
    }
}
