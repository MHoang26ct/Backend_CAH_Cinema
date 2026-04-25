package com.uit.backend_cinema.modules.notification.infrastructure.persistence;

import com.uit.backend_cinema.modules.notification.domain.repository.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Repository;

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
}
