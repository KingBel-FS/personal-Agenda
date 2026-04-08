package com.ia.api.task.repository;

import com.ia.api.task.domain.TaskOccurrenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskOccurrenceRepository extends JpaRepository<TaskOccurrenceEntity, UUID> {

    /** Chargement filtré par date (côté DB) — remplace le full-scan mémoire. */
    List<TaskOccurrenceEntity> findAllByUserIdAndStatusNotAndOccurrenceDateGreaterThanEqualOrderByOccurrenceDateAscOccurrenceTimeAsc(
            UUID userId, String status, LocalDate from);

    List<TaskOccurrenceEntity> findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
            UUID userId, String status, LocalDate from, LocalDate to);

    List<TaskOccurrenceEntity> findAllByTaskRuleIdAndOccurrenceDateGreaterThanEqual(UUID taskRuleId, LocalDate from);

    @Modifying
    @Query("DELETE FROM TaskOccurrenceEntity o WHERE o.taskRuleId = :ruleId AND o.occurrenceDate >= :from")
    void deleteByTaskRuleIdAndOccurrenceDateGreaterThanEqual(@Param("ruleId") UUID ruleId, @Param("from") LocalDate from);

    List<TaskOccurrenceEntity> findAllByTaskRuleIdAndOccurrenceDate(UUID taskRuleId, LocalDate occurrenceDate);

    List<TaskOccurrenceEntity> findAllByUserIdAndOccurrenceDateAndStatusNot(UUID userId, LocalDate occurrenceDate, String status);

    /** Batch : max date d'occurrence non-annulée par règle (remplace N+1 exists). */
    @Query("SELECT o.taskRuleId, MAX(o.occurrenceDate) FROM TaskOccurrenceEntity o " +
           "WHERE o.taskRuleId IN :ruleIds AND o.status <> 'canceled' GROUP BY o.taskRuleId")
    List<Object[]> findMaxOccurrenceDatePerRule(@Param("ruleIds") Collection<UUID> ruleIds);

    long countByUserIdAndOccurrenceDateBetweenAndStatus(UUID userId, LocalDate from, LocalDate to, String status);

    long countByUserIdAndTaskDefinitionIdAndOccurrenceDateBetweenAndStatus(
            UUID userId,
            UUID taskDefinitionId,
            LocalDate from,
            LocalDate to,
            String status
    );

    @Query(value = """
            SELECT
              COUNT(*) AS totalCount,
              COALESCE(SUM(CASE WHEN status = 'done' THEN 1 ELSE 0 END), 0) AS doneCount,
              COALESCE(SUM(CASE WHEN status = 'missed' THEN 1 ELSE 0 END), 0) AS missedCount,
              COALESCE(SUM(CASE WHEN status = 'skipped' THEN 1 ELSE 0 END), 0) AS skippedCount,
              COALESCE(SUM(CASE WHEN status = 'planned' THEN 1 ELSE 0 END), 0) AS plannedCount,
              COUNT(DISTINCT task_definition_id) AS distinctTaskCount
            FROM task_occurrences
            WHERE user_id = :userId
              AND status <> :excludedStatus
              AND occurrence_date BETWEEN :from AND :to
            """, nativeQuery = true)
    OccurrenceAggregateProjection aggregateByUserIdAndOccurrenceDateBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("excludedStatus") String excludedStatus
    );

    @Query(value = """
            SELECT
              o.task_definition_id AS taskDefinitionId,
              d.title AS title,
              d.icon AS icon,
              COUNT(*) AS totalCount,
              COALESCE(SUM(CASE WHEN o.status = 'done' THEN 1 ELSE 0 END), 0) AS doneCount,
              COALESCE(SUM(CASE WHEN o.status = 'missed' THEN 1 ELSE 0 END), 0) AS missedCount,
              COALESCE(SUM(CASE WHEN o.status = 'skipped' THEN 1 ELSE 0 END), 0) AS skippedCount,
              COALESCE(SUM(CASE WHEN o.status = 'planned' THEN 1 ELSE 0 END), 0) AS plannedCount
            FROM task_occurrences o
            JOIN task_definitions d ON d.id = o.task_definition_id
            WHERE o.user_id = :userId
              AND o.status <> :excludedStatus
              AND o.occurrence_date BETWEEN :from AND :to
            GROUP BY o.task_definition_id, d.title, d.icon
            ORDER BY COALESCE(SUM(CASE WHEN o.status = 'done' THEN 1 ELSE 0 END), 0) DESC,
                     COUNT(*) DESC,
                     d.title ASC
            """, nativeQuery = true)
    List<TaskStatsProjection> aggregateTaskStatsByUserIdAndOccurrenceDateBetween(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("excludedStatus") String excludedStatus
    );

    List<TaskOccurrenceEntity> findTop8ByUserIdAndTaskDefinitionIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateDescOccurrenceTimeDesc(
            UUID userId,
            UUID taskDefinitionId,
            String status,
            LocalDate from,
            LocalDate to
    );
}
