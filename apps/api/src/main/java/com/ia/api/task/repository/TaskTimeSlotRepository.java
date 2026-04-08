package com.ia.api.task.repository;

import com.ia.api.task.domain.TaskTimeSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskTimeSlotRepository extends JpaRepository<TaskTimeSlotEntity, UUID> {

    List<TaskTimeSlotEntity> findAllByTaskRuleIdOrderBySlotOrderAsc(UUID taskRuleId);

    List<TaskTimeSlotEntity> findAllByTaskRuleIdIn(Collection<UUID> ruleIds);

    void deleteAllByTaskRuleId(UUID taskRuleId);
}
