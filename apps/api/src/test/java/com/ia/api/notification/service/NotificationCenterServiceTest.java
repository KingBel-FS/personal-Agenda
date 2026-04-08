package com.ia.api.notification.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.notification.domain.NotificationCenterEntity;
import com.ia.api.notification.repository.NotificationCenterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCenterServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private NotificationCenterRepository notificationRepository;
    @Captor private ArgumentCaptor<NotificationCenterEntity> notifCaptor;

    private NotificationCenterService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        service = new NotificationCenterService(userRepository, notificationRepository);
        user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("gino@test.com");
    }

    @Test
    void list_returnsPagedNotifications() {
        var notif = new NotificationCenterEntity();
        notif.setId(UUID.randomUUID());
        notif.setUserId(user.getId());
        notif.setTitle("Test");
        notif.setStatus("RECEIVED");
        notif.setCreatedAt(Instant.now());

        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(eq(user.getId()), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(notif)));

        var result = service.list("gino@test.com", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test");
    }

    @Test
    void unviewedCount_returnsCorrectCount() {
        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(notificationRepository.countByUserIdAndStatus(user.getId(), "RECEIVED")).thenReturn(5L);

        long count = service.unviewedCount("gino@test.com");

        assertThat(count).isEqualTo(5);
    }

    @Test
    void markViewed_updatesStatusAndTimestamp() {
        var notif = new NotificationCenterEntity();
        notif.setId(UUID.randomUUID());
        notif.setUserId(user.getId());
        notif.setStatus("RECEIVED");

        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findById(notif.getId())).thenReturn(Optional.of(notif));

        service.markViewed("gino@test.com", notif.getId());

        verify(notificationRepository).save(notifCaptor.capture());
        assertThat(notifCaptor.getValue().getStatus()).isEqualTo("VIEWED");
        assertThat(notifCaptor.getValue().getViewedAt()).isNotNull();
    }

    @Test
    void markViewed_wrongUser_throws() {
        var notif = new NotificationCenterEntity();
        notif.setId(UUID.randomUUID());
        notif.setUserId(UUID.randomUUID()); // different user

        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findById(notif.getId())).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> service.markViewed("gino@test.com", notif.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void dismiss_updatesStatusAndTimestamp() {
        var notif = new NotificationCenterEntity();
        notif.setId(UUID.randomUUID());
        notif.setUserId(user.getId());
        notif.setStatus("VIEWED");

        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findById(notif.getId())).thenReturn(Optional.of(notif));

        service.dismiss("gino@test.com", notif.getId());

        verify(notificationRepository).save(notifCaptor.capture());
        assertThat(notifCaptor.getValue().getStatus()).isEqualTo("DISMISSED");
        assertThat(notifCaptor.getValue().getDismissedAt()).isNotNull();
    }

    @Test
    void dismiss_wrongUser_throws() {
        var notif = new NotificationCenterEntity();
        notif.setId(UUID.randomUUID());
        notif.setUserId(UUID.randomUUID()); // different user

        when(userRepository.findByEmailIgnoreCase("gino@test.com")).thenReturn(Optional.of(user));
        when(notificationRepository.findById(notif.getId())).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> service.dismiss("gino@test.com", notif.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void list_unknownUser_throws() {
        when(userRepository.findByEmailIgnoreCase("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.list("nobody@test.com", 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("introuvable");
    }
}
