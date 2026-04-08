package com.ia.api.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpActivationEmailSender implements ActivationEmailSender {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpActivationEmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@ia.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendActivationEmail(String email, String pseudo, String activationLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Active ton compte IA");
        message.setText("""
                Bonjour %s,

                Clique sur le lien ci-dessous pour activer ton compte IA :
                %s

                Ce lien est a usage unique et expire dans 1 heure.
                """.formatted(pseudo, activationLink));
        mailSender.send(message);
    }
}
