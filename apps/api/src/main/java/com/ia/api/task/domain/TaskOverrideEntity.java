package com.ia.api.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
    name = "task_overrides",
    // P-16 : contrainte unique reflétée dans l'entité JPA (cohérence avec Liquibase)
    uniqueConstraints = @UniqueConstraint(
        name = "uq_task_overrides_rule_date_slot",
        columnNames = {"task_rule_id", "occurrence_date", "task_time_slot_id"}
    )
)
public class TaskOverrideEntity {
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

    @Column(name = "override_action", nullable = false, length = 20)
    private String overrideAction;

    @Column(length = 100)
    private String title;

    @Column(length = 50)
    private String icon;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "time_mode", length = 20)
    private String timeMode;

    @Column(name = "fixed_time")
    private LocalTime fixedTime;

    @Column(name = "wake_up_offset_minutes")
    private Integer wakeUpOffsetMinutes;

    @Column(name = "task_time_slot_id")
    private UUID taskTimeSlotId;

    @Column(name = "override_status", length = 20)
    private String overrideStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** D-1 : optimistic locking. */
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
    public String getOverrideAction() { return overrideAction; }
    public void setOverrideAction(String overrideAction) { this.overrideAction = overrideAction; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTimeMode() { return timeMode; }
    public void setTimeMode(String timeMode) { this.timeMode = timeMode; }
    public LocalTime getFixedTime() { return fixedTime; }
    public void setFixedTime(LocalTime fixedTime) { this.fixedTime = fixedTime; }
    public Integer getWakeUpOffsetMinutes() { return wakeUpOffsetMinutes; }
    public void setWakeUpOffsetMinutes(Integer wakeUpOffsetMinutes) { this.wakeUpOffsetMinutes = wakeUpOffsetMinutes; }
    public UUID getTaskTimeSlotId() { return taskTimeSlotId; }
    public void setTaskTimeSlotId(UUID taskTimeSlotId) { this.taskTimeSlotId = taskTimeSlotId; }
    public String getOverrideStatus() { return overrideStatus; }
    public void setOverrideStatus(String overrideStatus) { this.overrideStatus = overrideStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
