package com.uit.backend_cinema.modules.auth.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.auth.api.dto.FullInvoiceDTO;
import com.uit.backend_cinema.modules.auth.api.dto.FullInvoiceDTO.FoodLineDTO;
import com.uit.backend_cinema.modules.auth.api.dto.FullInvoiceDTO.TicketDTO;
import com.uit.backend_cinema.modules.auth.api.dto.UserDTO;
import com.uit.backend_cinema.modules.auth.api.mapper.UserApiMapper;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.projection.BookingInvoiceRow;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaBookingRepository;

@Service
@Transactional(readOnly = true)
public class UserUseCase {

    private final UserRepository userRepository;
    private final JpaBookingRepository jpaBookingRepository;
    private final UserApiMapper userApiMapper;

    public UserUseCase(UserRepository userRepository,
            JpaBookingRepository jpaBookingRepository,
            UserApiMapper userApiMapper) {
        this.userRepository = userRepository;
        this.jpaBookingRepository = jpaBookingRepository;
        this.userApiMapper = userApiMapper;
    }

    // Trả về thông tin cơ bản của user + 5 hóa đơn gần nhất.
    public UserProfileResponseBundle getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));

        List<BookingInvoiceRow> rows = jpaBookingRepository.findTop5FullInvoicesByUserId(userId);
        List<FullInvoiceDTO> invoices = assembleInvoices(rows);

        return new UserProfileResponseBundle(userApiMapper.toDto(user), invoices);
    }

    // Cập nhật name / email / phone (chỉ field nào được gửi lên).
    @Transactional
    public UserDTO updateProfile(Long userId, String name, String email, String phone, String avatarUrl) {
        User updated = userRepository.updateProfile(userId, name, email, phone, avatarUrl);
        return userApiMapper.toDto(updated);
    }

    /**
     * Group flat rows theo booking_id, deduplicate seats và food items,
     * rồi build FullInvoiceDTO cho từng booking.
     */
    private List<FullInvoiceDTO> assembleInvoices(List<BookingInvoiceRow> rows) {
        // LinkedHashMap giữ thứ tự theo created_at DESC (truy vấn đã sort)
        Map<Long, InvoiceAccumulator> accMap = new LinkedHashMap<>();

        for (BookingInvoiceRow row : rows) {
            InvoiceAccumulator acc = accMap.computeIfAbsent(
                    row.getBookingId(), id -> new InvoiceAccumulator(row));
            acc.addRow(row);
        }

        List<FullInvoiceDTO> result = new ArrayList<>();
        for (InvoiceAccumulator acc : accMap.values()) {
            result.add(acc.build());
        }
        return result;
    }

    // Tích lũy các row thuộc cùng 1 booking, deduplicate seats & foods.
    private static class InvoiceAccumulator {
        private final BookingInvoiceRow header;
        private final Map<Long, TicketDTO> seats = new LinkedHashMap<>();
        private final Map<Long, FoodLineDTO> foods = new LinkedHashMap<>();

        InvoiceAccumulator(BookingInvoiceRow first) {
            this.header = first;
        }

        void addRow(BookingInvoiceRow row) {
            // Deduplicate ghế
            if (row.getSeatId() != null && !seats.containsKey(row.getSeatId())) {
                seats.put(row.getSeatId(), TicketDTO.builder()
                        .seatId(row.getSeatId())
                        .seatRow(row.getSeatRow())
                        .seatCol(row.getSeatCol())
                        .seatType(row.getSeatType())
                        .ticketPrice(row.getTicketPrice())
                        .build());
            }
            // Deduplicate thức ăn
            if (row.getFoodId() != null && !foods.containsKey(row.getFoodId())) {
                foods.put(row.getFoodId(), FoodLineDTO.builder()
                        .foodId(row.getFoodId())
                        .foodName(row.getFoodName())
                        .foodImageUrl(row.getFoodImageUrl())
                        .foodCategory(row.getFoodCategory())
                        .quantity(row.getFoodQuantity() != null ? row.getFoodQuantity() : 0)
                        .unitPrice(row.getFoodUnitPrice())
                        .build());
            }
        }

        FullInvoiceDTO build() {
            return FullInvoiceDTO.builder()
                    .bookingId(header.getBookingId())
                    .bookingStatus(header.getBookingStatus())
                    .paymentMethod(header.getPaymentMethod())
                    .discountAmount(header.getDiscountAmount())
                    .totalPrice(header.getTotalPrice())
                    .bookingCreatedAt(header.getBookingCreatedAt())
                    .voucherCode(header.getVoucherCode())
                    .showtimeId(header.getShowtimeId())
                    .movieFormat(header.getMovieFormat())
                    .startTime(header.getStartTime())
                    .endTime(header.getEndTime())
                    .movieId(header.getMovieId())
                    .movieTitle(header.getMovieTitle())
                    .moviePosterUrl(header.getMoviePosterUrl())
                    .cinemaName(header.getCinemaName())
                    .roomName(header.getRoomName())
                    .seats(new ArrayList<>(seats.values()))
                    .foods(new ArrayList<>(foods.values()))
                    .foodTotalPrice(header.getFoodTotalPrice())
                    .build();
        }
    }

    public record UserProfileResponseBundle(UserDTO user, List<FullInvoiceDTO> recentInvoices) {
    }
}
