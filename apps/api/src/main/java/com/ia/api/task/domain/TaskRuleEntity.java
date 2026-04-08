package com.ia.api.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "task_rules")
public class TaskRuleEntity {
    @Id
    private UUID id;

    @Column(name = "task_definition_id", nullable = false)
    private UUID taskDefinitionId;

    @Column(name = "day_categories", nullable = false, length = 200)
    private String dayCategories;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "time_mode", nullable = false, length = 20)
    private String timeMode;

    @Column(name = "fixed_time")
    private LocalTime fixedTime;

    @Column(name = "wake_up_offset_minutes")
    private Integer wakeUpOffsetMinutes;

    @Column(name = "recurrence_type", length = 20)
    private String recurrenceType;

    @Column(name = "days_of_week", length = 20)
    private String daysOfWeek;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTaskDefinitionId() { return taskDefinitionId; }
    public void setTaskDefinitionId(UUID taskDefinitionId) { this.taskDefinitionId = taskDefinitionId; }

    public String getDayCategories() { return dayCategories; }
    public void setDayCategories(String dayCategories) { this.dayCategories = dayCategories; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getTimeMode() { return timeMode; }
    public void setTimeMode(String timeMode) { this.timeMode = timeMode; }

    public LocalTime getFixedTime() { return fixedTime; }
    public void setFixedTime(LocalTime fixedTime) { this.fixedTime = fixedTime; }

    public Integer getWakeUpOffsetMinutes() { return wakeUpOffsetMinutes; }
    public void setWakeUpOffsetMinutes(Integer wakeUpOffsetMinutes) { this.wakeUpOffsetMinutes = wakeUpOffsetMinutes; }

    public String getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(String recurrenceType) { this.recurrenceType = recurrenceType; }

    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
