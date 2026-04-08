package com.ia.api.goal.repository;

import com.ia.api.goal.domain.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {
    List<GoalEntity> findAllByUserIdAndActiveTrueOrderByPeriodTypeAscCreatedAtAsc(UUID userId);
    Optional<GoalEntity> findByIdAndUserId(UUID id, UUID userId);
    List<GoalEntity> findAllByUserId(UUID userId);
}
