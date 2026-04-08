package com.ia.api.task.repository;

import com.ia.api.task.domain.TaskRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskRuleRepository extends JpaRepository<TaskRuleEntity, UUID> {
    Optional<TaskRuleEntity> findByTaskDefinitionId(UUID taskDefinitionId);
    java.util.List<TaskRuleEntity> findAllByTaskDefinitionIdIn(Iterable<UUID> taskDefinitionIds);
    void deleteAllByTaskDefinitionIdIn(Iterable<UUID> taskDefinitionIds);
}
