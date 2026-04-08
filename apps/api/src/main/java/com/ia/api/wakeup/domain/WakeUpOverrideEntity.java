package com.ia.api.wakeup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "user_day_wake_up_override")
public class WakeUpOverrideEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "override_date", nullable = false)
    private LocalDate overrideDate;

    @Column(name = "wake_up_time", nullable = false)
    private LocalTime wakeUpTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public LocalDate getOverrideDate() { return overrideDate; }
    public void setOverrideDate(LocalDate overrideDate) { this.overrideDate = overrideDate; }

    public LocalTime getWakeUpTime() { return wakeUpTime; }
    public void setWakeUpTime(LocalTime wakeUpTime) { this.wakeUpTime = wakeUpTime; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
