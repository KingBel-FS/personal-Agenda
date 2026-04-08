package com.ia.api.notification.repository;

import com.ia.api.notification.domain.NotificationJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationJobRepository extends JpaRepository<NotificationJobEntity, UUID> {

    List<NotificationJobEntity> findAllByOccurrenceIdOrderByScheduledAtAsc(UUID occurrenceId);

    List<NotificationJobEntity> findAllByUserIdAndStatusOrderByScheduledAtAsc(UUID userId, String status);
}
