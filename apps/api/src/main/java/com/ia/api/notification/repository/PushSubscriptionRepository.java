package com.ia.api.notification.repository;

import com.ia.api.notification.domain.PushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, UUID> {

    Optional<PushSubscriptionEntity> findByUserIdAndEndpoint(UUID userId, String endpoint);

    List<PushSubscriptionEntity> findAllByUserIdAndRevokedAtIsNull(UUID userId);

    List<PushSubscriptionEntity> findAllByRevokedAtIsNotNull();
}
