package com.ia.api.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "day_profiles")
public class DayProfileEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_category", nullable = false)
    private DayCategory dayCategory;

    @Column(name = "wake_up_time", nullable = false)
    private LocalTime wakeUpTime;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public DayCategory getDayCategory() {
        return dayCategory;
    }

    public void setDayCategory(DayCategory dayCategory) {
        this.dayCategory = dayCategory;
    }

    public LocalTime getWakeUpTime() {
        return wakeUpTime;
    }

    public void setWakeUpTime(LocalTime wakeUpTime) {
        this.wakeUpTime = wakeUpTime;
    }
}
