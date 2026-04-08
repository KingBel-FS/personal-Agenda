package com.ia.api.focuslock.repository;

import com.ia.api.focuslock.domain.FlRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlRuleRepository extends JpaRepository<FlRuleEntity, UUID> {
    List<FlRuleEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<FlRuleEntity> findByIdAndUserId(UUID id, UUID userId);
    long countByUserIdAndActiveTrue(UUID userId);
}
