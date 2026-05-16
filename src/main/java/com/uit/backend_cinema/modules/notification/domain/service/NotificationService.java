package com.uit.backend_cinema.modules.notification.domain.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.QRCodeUtil;
import com.uit.backend_cinema.modules.notification.domain.repository.EmailSender;
import com.uit.backend_cinema.modules.notification.domain.repository.OtpStorage;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;

@Service
public class NotificationService {
    private final EmailSender emailSender;
    private final OtpStorage otpStorage;

    private static final long OTP_VALID_DURATION = 5;
    private static final int QR_SIZE = 300;

    public NotificationService(EmailSender emailSender, OtpStorage otpStorage) {
        this.emailSender = emailSender;
        this.otpStorage = otpStorage;
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

    public void sendTicketEmail(String email, Long bookingId, List<Ticket> tickets) {
        String subject = "Vé xem phim của bạn - Booking #" + bookingId;
        String content = buildTicketEmailContent(bookingId, tickets);

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

    private String buildTicketEmailContent(Long bookingId, List<Ticket> tickets) {
        StringBuilder content = new StringBuilder();
        content.append("Cảm ơn bạn đã thanh toán thành công.\n\n");
        content.append("Booking ID: ").append(bookingId).append("\n");
        content.append("Danh sách vé:\n");

        for (Ticket ticket : tickets) {
            content.append("- Ticket #")
                    .append(ticket.getTicketId())
                    .append(", Seat #")
                    .append(ticket.getSeatId())
                    .append("\n");
        }

        content.append("\n=== Mã QR ===\n");
        content.append("Mã QR của từng vé đã được đính kèm trong email này (ticket-<id>.png).\n");
        content.append("Vui lòng xuất trình mã QR khi vào rạp.\n");
        return content.toString();
    }

    /**
     * Tạo chuỗi nội dung để mã hoá vào QR Code của một vé.
     */
    private String buildQRText(Ticket ticket, Long bookingId) {
        return "TICKET:" + ticket.getTicketId()
                + ":BOOKING:" + bookingId
                + ":SEAT:" + ticket.getSeatId();
    }
}

