package com.ia.api.notification.service;

import com.ia.api.auth.repository.UserRepository;
import com.ia.api.notification.domain.NotificationCenterEntity;
import com.ia.api.notification.repository.NotificationCenterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationCenterService {

    private final UserRepository userRepository;
    private final NotificationCenterRepository notificationRepository;

    public NotificationCenterService(
            UserRepository userRepository,
            NotificationCenterRepository notificationRepository
    ) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    public Page<NotificationCenterEntity> list(String email, int page, int size) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(page, size));
    }

    public long unviewedCount(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return notificationRepository.countByUserIdAndStatus(user.getId(), "RECEIVED");
    }

    @Transactional
    public void markViewed(String email, UUID notificationId) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable."));
        if (!notification.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification introuvable.");
        }
        notification.setStatus("VIEWED");
        notification.setViewedAt(Instant.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void dismiss(String email, UUID notificationId) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable."));
        if (!notification.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification introuvable.");
        }
        notification.setStatus("DISMISSED");
        notification.setDismissedAt(Instant.now());
        notificationRepository.save(notification);
    }
}
