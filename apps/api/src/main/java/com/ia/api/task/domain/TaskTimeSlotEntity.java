package com.ia.api.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
    name = "task_time_slots",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_task_time_slots_rule_order",
        columnNames = {"task_rule_id", "slot_order"}
    )
)
public class TaskTimeSlotEntity {

    @Id
    private UUID id;

    @Column(name = "task_rule_id", nullable = false)
    private UUID taskRuleId;

    @Column(name = "slot_order", nullable = false)
    private int slotOrder;

    /** FIXED | WAKE_UP_OFFSET | AFTER_PREVIOUS */
    @Column(name = "time_mode", nullable = false, length = 20)
    private String timeMode;

    @Column(name = "fixed_time")
    private LocalTime fixedTime;

    @Column(name = "wake_up_offset_minutes")
    private Integer wakeUpOffsetMinutes;

    @Column(name = "after_previous_minutes")
    private Integer afterPreviousMinutes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTaskRuleId() { return taskRuleId; }
    public void setTaskRuleId(UUID taskRuleId) { this.taskRuleId = taskRuleId; }

    public int getSlotOrder() { return slotOrder; }
    public void setSlotOrder(int slotOrder) { this.slotOrder = slotOrder; }

    public String getTimeMode() { return timeMode; }
    public void setTimeMode(String timeMode) { this.timeMode = timeMode; }

    public LocalTime getFixedTime() { return fixedTime; }
    public void setFixedTime(LocalTime fixedTime) { this.fixedTime = fixedTime; }

    public Integer getWakeUpOffsetMinutes() { return wakeUpOffsetMinutes; }
    public void setWakeUpOffsetMinutes(Integer wakeUpOffsetMinutes) { this.wakeUpOffsetMinutes = wakeUpOffsetMinutes; }

    public Integer getAfterPreviousMinutes() { return afterPreviousMinutes; }
    public void setAfterPreviousMinutes(Integer afterPreviousMinutes) { this.afterPreviousMinutes = afterPreviousMinutes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
