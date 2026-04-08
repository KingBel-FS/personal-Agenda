package com.ia.api.today;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<BadgeEntity, UUID> {

    List<BadgeEntity> findAllByUserId(UUID userId);

    boolean existsByUserIdAndBadgeType(UUID userId, String badgeType);
}
