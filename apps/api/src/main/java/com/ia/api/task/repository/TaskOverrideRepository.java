package com.ia.api.task.repository;

import com.ia.api.task.domain.TaskOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskOverrideRepository extends JpaRepository<TaskOverrideEntity, UUID> {
    Optional<TaskOverrideEntity> findByTaskRuleIdAndOccurrenceDate(UUID taskRuleId, LocalDate occurrenceDate);

    Optional<TaskOverrideEntity> findByTaskRuleIdAndOccurrenceDateAndTaskTimeSlotId(UUID taskRuleId, LocalDate occurrenceDate, UUID taskTimeSlotId);

    List<TaskOverrideEntity> findAllByTaskRuleIdInAndOccurrenceDateBetween(
            Collection<UUID> taskRuleIds,
            LocalDate from,
            LocalDate to
    );

    List<TaskOverrideEntity> findAllByTaskRuleIdIn(Collection<UUID> taskRuleIds);

    /** P-17 : purge des overrides orphelins après suppression THIS_AND_FOLLOWING. */
    void deleteAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(UUID taskRuleId, LocalDate from);
}
