package com.ia.api.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpPasswordResetEmailSenderTest {
    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @Test
    void sendsResetEmailViaMailSender() {
        SmtpPasswordResetEmailSender sender = new SmtpPasswordResetEmailSender(mailSender, "no-reply@ia.local");

        sender.sendPasswordResetEmail("alice@example.com", "alice", "http://localhost:4200/reset-password?token=123");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getSubject()).isEqualTo("Reinitialise ton mot de passe IA");
        assertThat(message.getText()).contains("http://localhost:4200/reset-password?token=123");
    }
}
