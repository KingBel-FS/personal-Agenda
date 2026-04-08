package com.ia.api.notification.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.notification.domain.PushSubscriptionEntity;
import com.ia.api.notification.repository.PushSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PushSubscriptionRepository subscriptionRepository;
    @Captor private ArgumentCaptor<PushSubscriptionEntity> subCaptor;

    private PushSubscriptionService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        service = new PushSubscriptionService(userRepository, subscriptionRepository, "test-vapid-public-key");
        user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("gino@test.com");
    }

    @Test
    void getVapidPublicKey_returnsConfiguredKey() {
        assertThat(service.getVapidPublicKey()).isEqualTo("test-vapid-public-key");
    }

    @Test
    void subscribe_createsNewSubscription() {
        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndEndpoint(user.getId(), "https://push.example.com/sub1"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.subscribe("gino@test.com", "https://push.example.com/sub1", "auth123", "p256dh456");

        verify(subscriptionRepository).save(subCaptor.capture());
        var saved = subCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getEndpoint()).isEqualTo("https://push.example.com/sub1");
        assertThat(saved.getAuthKey()).isEqualTo("auth123");
        assertThat(saved.getP256dhKey()).isEqualTo("p256dh456");
        assertThat(saved.getRevokedAt()).isNull();
        assertThat(result).isNotNull();
    }

    @Test
    void subscribe_reactivatesRevokedSubscription() {
        var existing = new PushSubscriptionEntity();
        existing.setId(UUID.randomUUID());
        existing.setUserId(user.getId());
        existing.setEndpoint("https://push.example.com/sub1");
        existing.setAuthKey("old-auth");
        existing.setP256dhKey("old-p256dh");
        existing.setRevokedAt(Instant.now());

        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndEndpoint(user.getId(), "https://push.example.com/sub1"))
                .thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.subscribe("gino@test.com", "https://push.example.com/sub1", "new-auth", "new-p256dh");

        verify(subscriptionRepository).save(subCaptor.capture());
        var saved = subCaptor.getValue();
        assertThat(saved.getAuthKey()).isEqualTo("new-auth");
        assertThat(saved.getP256dhKey()).isEqualTo("new-p256dh");
        assertThat(saved.getRevokedAt()).isNull();
    }

    @Test
    void subscribe_unknownUser_throws() {
        when(userRepository.findByEmailIgnoreCase("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.subscribe("nobody@test.com", "https://push.example.com/sub1", "a", "b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void unsubscribe_revokesExistingSubscription() {
        var existing = new PushSubscriptionEntity();
        existing.setId(UUID.randomUUID());
        existing.setUserId(user.getId());
        existing.setEndpoint("https://push.example.com/sub1");

        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndEndpoint(user.getId(), "https://push.example.com/sub1"))
                .thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.unsubscribe("gino@test.com", "https://push.example.com/sub1");

        verify(subscriptionRepository).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getRevokedAt()).isNotNull();
    }

    @Test
    void unsubscribe_nonExistentEndpoint_noOp() {
        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdAndEndpoint(user.getId(), "https://unknown.com"))
                .thenReturn(Optional.empty());

        // Should not throw
        service.unsubscribe("gino@test.com", "https://unknown.com");
    }

    @Test
    void purgeRevoked_deletesAllRevokedSubscriptions() {
        var revoked1 = new PushSubscriptionEntity();
        revoked1.setId(UUID.randomUUID());
        revoked1.setRevokedAt(Instant.now());
        var revoked2 = new PushSubscriptionEntity();
        revoked2.setId(UUID.randomUUID());
        revoked2.setRevokedAt(Instant.now());

        when(subscriptionRepository.findAllByRevokedAtIsNotNull()).thenReturn(List.of(revoked1, revoked2));

        int count = service.purgeRevoked();

        assertThat(count).isEqualTo(2);
        verify(subscriptionRepository).deleteAll(List.of(revoked1, revoked2));
    }
}
