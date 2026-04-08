package com.ia.api.notification.repository;

import com.ia.api.notification.domain.NotificationCenterEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationCenterRepository extends JpaRepository<NotificationCenterEntity, UUID> {

    Page<NotificationCenterEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, String status);
}
