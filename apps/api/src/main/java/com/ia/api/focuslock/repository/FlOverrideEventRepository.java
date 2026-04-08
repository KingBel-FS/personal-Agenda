package com.ia.api.focuslock.repository;

import com.ia.api.focuslock.domain.FlOverrideEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlOverrideEventRepository extends JpaRepository<FlOverrideEventEntity, UUID> {
    List<FlOverrideEventEntity> findByUserIdOrderByOverriddenAtDesc(UUID userId);
    long countByRuleId(UUID ruleId);
}
