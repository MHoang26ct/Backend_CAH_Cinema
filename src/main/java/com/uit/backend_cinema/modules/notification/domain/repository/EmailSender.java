package com.uit.backend_cinema.modules.notification.domain.repository;

import java.util.Map;

public interface EmailSender {
    void sendEmail(String to, String subject, String content);

    /**
     * Gửi email kèm các file đính kèm.
     *
     * @param to          Địa chỉ email người nhận
     * @param subject     Tiêu đề email
     * @param content     Nội dung email (text)
     * @param attachments Map với key là tên file (VD: "ticket-1.png") và value là byte[] của file
     */
    void sendEmailWithAttachments(String to, String subject, String content, Map<String, byte[]> attachments);
}

