package com.uit.backend_cinema.modules.notification.domain.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.JwtUtil;
import com.uit.backend_cinema.common.util.QRCodeUtil;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.notification.domain.repository.EmailSender;
import com.uit.backend_cinema.modules.notification.domain.repository.OtpStorage;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;

@Service
public class NotificationService {
    private final EmailSender emailSender;
    private final OtpStorage otpStorage;
    private final JwtUtil jwtUtil;

    private static final long OTP_VALID_DURATION = 5;
    private static final int QR_SIZE = 300;

    public NotificationService(EmailSender emailSender, OtpStorage otpStorage, JwtUtil jwtUtil) {
        this.emailSender = emailSender;
        this.otpStorage = otpStorage;
        this.jwtUtil = jwtUtil;
    }

    public void sendOtp(String email) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        otpStorage.save("OTP: " + email, otp, OTP_VALID_DURATION);
        emailSender.sendEmail(email, "OTP", "Mã xác nhận của bạn: " + otp);
    }

    public boolean verifyOtp(String email, String otp) {
        String key = "OTP: " + email;
        String storedOtp = otpStorage.get(key);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStorage.delete(key);
            return true;
        }
        else {
            throw new BusinessException("OTP không hợp lệ hoặc đã hết hạn", ErrorCode.OTP_INVALID);
        }
    }

    public void sendTicketEmail(String email, Long bookingId, Movie movie, Showtime showtime, List<Ticket> tickets, List<Seat> seats) {
        String subject = "Vé xem phim của bạn - Booking #" + bookingId;
        String content = buildTicketEmailContent(bookingId, movie, showtime, tickets, seats);

        // Tạo QR Code cho từng vé và gom vào Map<tênFile, byte[]>
        Map<String, byte[]> attachments = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            String qrText = buildQRText(ticket, bookingId);
            String fileName = "ticket-" + ticket.getTicketId() + ".png";
            byte[] qrImage = QRCodeUtil.generateQRCodeImage(qrText, QR_SIZE, QR_SIZE);
            attachments.put(fileName, qrImage);
        }

        emailSender.sendEmailWithAttachments(email, subject, content, attachments);
    }

    private String buildTicketEmailContent(Long bookingId, Movie movie, Showtime showtime, List<Ticket> tickets, List<Seat> seats) {
        StringBuilder content = new StringBuilder();
        content.append("Cảm ơn bạn đã thanh toán thành công.\n\n");
        content.append("Booking ID: ").append(bookingId).append("\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        content.append("Phim: ").append(movie.getTitle()).append("\n");
        content.append("Suất chiếu: ").append(showtime.getStartTime().format(formatter)).append("\n");
        content.append("Danh sách vé:\n");

        Map<Long, Seat> seatMap = seats.stream().collect(Collectors.toMap(Seat::getSeatId, seat -> seat));

        for (Ticket ticket : tickets) {
            Seat seat = seatMap.get(ticket.getSeatId());
            String seatName = getSeatName(seat);
            content.append("- Ticket #")
                    .append(ticket.getTicketId())
                    .append(", Ghế: ")
                    .append(seatName)
                    .append("\n");
        }

        content.append("\n=== Mã QR ===\n");
        content.append("Mã QR của từng vé đã được đính kèm trong email này (ticket-<id>.png).\n");
        content.append("Vui lòng xuất trình mã QR khi vào rạp.\n");
        return content.toString();
    }

    private String getSeatName(Seat seat) {
        if (seat == null) return "N/A";
        char rowLetter = (char) ('A' + seat.getSeatRow().intValue() - 1);
        return rowLetter + String.valueOf(seat.getSeatCol().intValue());
    }

    /**
     * Tạo chuỗi nội dung để mã hoá vào QR Code của một vé.
     */
    private String buildQRText(Ticket ticket, Long bookingId) {
        return jwtUtil.generateTicketQrToken(ticket.getTicketId(), bookingId, ticket.getShowtimeId());
    }

    /**
     * Gửi email thông báo hủy suất chiếu do bảo trì / sự cố.
     * Booking sẽ được hoàn tiền; admin xử lý hoàn tiền thực tế ngoài hệ thống.
     */
    public void sendShowtimeCancelledEmail(String email, Long bookingId,
                                           Movie movie, Showtime showtime, String reason) {
        String subject = "[CAH Cinema] Thông báo hủy suất chiếu - Booking #" + bookingId;
        String content = buildShowtimeCancelledEmailContent(bookingId, movie, showtime, reason);
        emailSender.sendEmail(email, subject, content);
    }

    private String buildShowtimeCancelledEmailContent(Long bookingId, Movie movie,
                                                      Showtime showtime, String reason) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        StringBuilder content = new StringBuilder();
        content.append("Kính gửi Quý khách,\n\n");
        content.append("Chúng tôi xin thông báo suất chiếu sau đã bị hủy:\n\n");
        content.append("Booking ID: ").append(bookingId).append("\n");
        content.append("Phim: ").append(movie.getTitle()).append("\n");
        content.append("Suất chiếu: ").append(showtime.getStartTime().format(formatter)).append("\n");
        content.append("Lý do: ").append(reason != null ? reason : "Sự cố kỹ thuật").append("\n\n");
        content.append("Đơn đặt vé của bạn đã được hoàn tiền. ");
        content.append("Vui lòng liên hệ hotline để biết thêm chi tiết.\n\n");
        content.append("Chúng tôi xin lỗi vì sự bất tiện này.\n");
        content.append("Trân trọng,\nCAH Cinema");
        return content.toString();
    }
}


