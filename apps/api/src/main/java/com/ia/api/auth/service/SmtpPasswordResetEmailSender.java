package com.ia.api.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpPasswordResetEmailSender implements PasswordResetEmailSender {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpPasswordResetEmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@ia.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendPasswordResetEmail(String email, String pseudo, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Reinitialise ton mot de passe IA");
        message.setText("""
                Bonjour %s,

                Clique sur le lien ci-dessous pour reinitialiser ton mot de passe IA :
                %s

                Ce lien est a usage unique et expire dans 1 heure.
                """.formatted(pseudo, resetLink));
        mailSender.send(message);
    }
}
