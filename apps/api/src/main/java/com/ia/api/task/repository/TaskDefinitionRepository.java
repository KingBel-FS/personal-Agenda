package com.ia.api.task.repository;

import com.ia.api.task.domain.TaskDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskDefinitionRepository extends JpaRepository<TaskDefinitionEntity, UUID> {
    List<TaskDefinitionEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    void deleteAllByUserId(UUID userId);
}
