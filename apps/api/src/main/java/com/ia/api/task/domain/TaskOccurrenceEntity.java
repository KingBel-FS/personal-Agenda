package com.ia.api.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "task_occurrences")
public class TaskOccurrenceEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "task_definition_id", nullable = false)
    private UUID taskDefinitionId;

    @Column(name = "task_rule_id", nullable = false)
    private UUID taskRuleId;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    @Column(name = "occurrence_time", nullable = false)
    private LocalTime occurrenceTime;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "day_category", nullable = false, length = 30)
    private String dayCategory;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Slot intra-journée (null = comportement legacy mono-slot). */
    @Column(name = "task_time_slot_id")
    private UUID taskTimeSlotId;

    /** D-1 : optimistic locking — évite les last-write-wins silencieux. */
    @Version
    @Column(nullable = false)
    private Long version;

    /** D-4 : UUID garanti avant toute persistance. */
    @PrePersist
    private void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getTaskDefinitionId() { return taskDefinitionId; }
    public void setTaskDefinitionId(UUID taskDefinitionId) { this.taskDefinitionId = taskDefinitionId; }
    public UUID getTaskRuleId() { return taskRuleId; }
    public void setTaskRuleId(UUID taskRuleId) { this.taskRuleId = taskRuleId; }
    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(LocalDate occurrenceDate) { this.occurrenceDate = occurrenceDate; }
    public LocalTime getOccurrenceTime() { return occurrenceTime; }
    public void setOccurrenceTime(LocalTime occurrenceTime) { this.occurrenceTime = occurrenceTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDayCategory() { return dayCategory; }
    public void setDayCategory(String dayCategory) { this.dayCategory = dayCategory; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public UUID getTaskTimeSlotId() { return taskTimeSlotId; }
    public void setTaskTimeSlotId(UUID taskTimeSlotId) { this.taskTimeSlotId = taskTimeSlotId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
