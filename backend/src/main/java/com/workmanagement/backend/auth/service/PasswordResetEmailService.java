package com.workmanagement.backend.auth.service;

import com.workmanagement.backend.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /** Hỗ trợ UC-1.3 — Gửi link đặt lại mật khẩu qua email */
    public void sendResetLink(String toEmail, String resetToken) {
        String resetLink = appProperties.getFrontendBaseUrl() + "/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Đặt lại mật khẩu - Work Management System");
        message.setText(buildBody(resetLink));

        try {
            mailSender.send(message);
            log.info("Đã gửi email khôi phục mật khẩu tới {}", toEmail);
        } catch (MailException ex) {
            log.error("Không thể gửi email khôi phục mật khẩu tới {}", toEmail, ex);
        }
    }

    private String buildBody(String resetLink) {
        return """
                Xin chào,

                Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản Work Management System.

                Nhấn vào link sau để đặt mật khẩu mới (link có hiệu lực trong 15 phút):
                %s

                Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.

                Trân trọng,
                Work Management System
                """.formatted(resetLink);
    }

}
