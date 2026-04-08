package com.ia.api.focuslock.repository;

import com.ia.api.focuslock.domain.FlScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlScheduleRepository extends JpaRepository<FlScheduleEntity, UUID> {
    List<FlScheduleEntity> findByRuleId(UUID ruleId);
    void deleteByRuleId(UUID ruleId);
}
