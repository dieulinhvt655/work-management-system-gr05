package com.workmanagement.backend.auth.service;

import com.workmanagement.backend.common.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private PasswordResetEmailService passwordResetEmailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetEmailService, "fromEmail", "noreply@test.com");
        when(appProperties.getFrontendBaseUrl()).thenReturn("http://localhost:5173");
    }

    @Test
    void sendResetLink_shouldSendEmailWithResetUrl() {
        passwordResetEmailService.sendResetLink("user@test.com", "reset-token-abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("user@test.com");
        assertThat(message.getFrom()).isEqualTo("noreply@test.com");
        assertThat(message.getText()).contains("http://localhost:5173/reset-password?token=reset-token-abc");
    }

    @Test
    void sendResetLink_shouldNotThrowWhenMailFails() {
        when(appProperties.getFrontendBaseUrl()).thenReturn("http://localhost:5173");
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        passwordResetEmailService.sendResetLink("user@test.com", "token");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

}
