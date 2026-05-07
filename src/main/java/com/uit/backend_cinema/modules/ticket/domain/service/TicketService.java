package com.uit.backend_cinema.modules.ticket.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.ticket.domain.entity.PendingTicketItem;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.repository.PendingTicketItemRepository;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final PendingTicketItemRepository pendingTicketItemRepository;

    public TicketService(TicketRepository ticketRepository,
                         PendingTicketItemRepository pendingTicketItemRepository) {
        this.ticketRepository = ticketRepository;
        this.pendingTicketItemRepository = pendingTicketItemRepository;
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
    public List<Ticket> finalizeTicketsForPaidBooking(Long bookingId) {
        List<PendingTicketItem> pendingItems = pendingTicketItemRepository.findAllActiveByBookingId(bookingId);
        List<Ticket> tickets = new ArrayList<>();
        for (PendingTicketItem item : pendingItems) {
            Ticket ticket = new Ticket();
            ticket.setBookingId(item.getBookingId());
            ticket.setSeatId(item.getSeatId());
            ticket.setPrice(item.getUnitPrice());
            tickets.add(ticket);
        }

        if (tickets.isEmpty()) {
            tickets = findAllByBookingId(bookingId);
            if (tickets.isEmpty()) {
                throw new BusinessException("Booking không có vé nháp để tạo vé", ErrorCode.RESOURCE_NOT_FOUND);
            }
            return tickets;
        }

        createTicketIfAbsent(tickets);
        return findAllByBookingId(bookingId);
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
            if (ticket.getBookingId() == null || ticket.getSeatId() == null) {
                throw new BusinessException("BookingId và SeatId không được null", ErrorCode.VALIDATION_FAILED);
            }
        });
        Long bookingId = tickets.get(0).getBookingId();
        if (!tickets.stream().allMatch(ticket -> ticket.getBookingId().equals(bookingId))) {
            throw new BusinessException("Tất cả các vé phải có cùng một bookingId", ErrorCode.VALIDATION_FAILED);
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
}
