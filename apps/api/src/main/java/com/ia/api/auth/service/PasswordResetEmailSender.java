package com.ia.api.auth.service;

public interface PasswordResetEmailSender {
    void sendPasswordResetEmail(String email, String pseudo, String resetLink);
}
