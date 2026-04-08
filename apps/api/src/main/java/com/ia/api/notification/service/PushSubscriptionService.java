package com.ia.api.notification.service;

import com.ia.api.auth.repository.UserRepository;
import com.ia.api.notification.domain.PushSubscriptionEntity;
import com.ia.api.notification.repository.PushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PushSubscriptionService {

    private final UserRepository userRepository;
    private final PushSubscriptionRepository subscriptionRepository;
    private final String vapidPublicKey;

    public PushSubscriptionService(
            UserRepository userRepository,
            PushSubscriptionRepository subscriptionRepository,
            @Value("${app.vapid.public-key}") String vapidPublicKey
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.vapidPublicKey = vapidPublicKey;
    }

    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Transactional
    public PushSubscriptionEntity subscribe(String email, String endpoint, String authKey, String p256dhKey) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        // Upsert: if subscription exists for this user+endpoint, reactivate it
        var existing = subscriptionRepository.findByUserIdAndEndpoint(user.getId(), endpoint);
        if (existing.isPresent()) {
            var sub = existing.get();
            sub.setAuthKey(authKey);
            sub.setP256dhKey(p256dhKey);
            sub.setRevokedAt(null);
            sub.setUpdatedAt(Instant.now());
            return subscriptionRepository.save(sub);
        }

        var sub = new PushSubscriptionEntity();
        sub.setId(UUID.randomUUID());
        sub.setUserId(user.getId());
        sub.setEndpoint(endpoint);
        sub.setAuthKey(authKey);
        sub.setP256dhKey(p256dhKey);
        sub.setCreatedAt(Instant.now());
        sub.setUpdatedAt(Instant.now());
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(String email, String endpoint) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        subscriptionRepository.findByUserIdAndEndpoint(user.getId(), endpoint)
                .ifPresent(sub -> {
                    sub.setRevokedAt(Instant.now());
                    sub.setUpdatedAt(Instant.now());
                    subscriptionRepository.save(sub);
                });
    }

    @Transactional
    public int purgeRevoked() {
        var revoked = subscriptionRepository.findAllByRevokedAtIsNotNull();
        subscriptionRepository.deleteAll(revoked);
        return revoked.size();
    }
}
