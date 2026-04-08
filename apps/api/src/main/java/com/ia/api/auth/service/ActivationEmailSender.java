package com.ia.api.auth.service;

public interface ActivationEmailSender {
    void sendActivationEmail(String email, String pseudo, String activationLink);
}
