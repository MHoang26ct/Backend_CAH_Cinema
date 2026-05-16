package com.uit.backend_cinema.modules.notification.infrastructure.persistence;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.notification.domain.repository.EmailSender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GmailSender implements EmailSender {
    private final JavaMailSender mailSender;

    @Value("${GMAIL_USERNAME}")
    private String email;

    @Override
    public void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(email);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    @Override
    public void sendEmailWithAttachments(String to, String subject, String content, Map<String, byte[]> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = bật chế độ multipart để hỗ trợ đính kèm file
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(email);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);

            for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
                helper.addAttachment(entry.getKey(), new ByteArrayResource(entry.getValue()));
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Không thể gửi email có đính kèm tới: " + to, e);
        }
    }
}

