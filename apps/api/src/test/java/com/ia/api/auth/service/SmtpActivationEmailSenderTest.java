package com.ia.api.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpActivationEmailSenderTest {
    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @Test
    void sendsActivationEmailViaMailSender() {
        SmtpActivationEmailSender sender = new SmtpActivationEmailSender(mailSender, "no-reply@ia.local");

        sender.sendActivationEmail("alice@example.com", "alice", "http://localhost:4200/activate?token=123");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@ia.local");
        assertThat(message.getTo()).containsExactly("alice@example.com");
        assertThat(message.getSubject()).isEqualTo("Active ton compte IA");
        assertThat(message.getText()).contains("http://localhost:4200/activate?token=123");
    }
}
