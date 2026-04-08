package com.ia.api.focuslock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fl_usage_event")
public class FlUsageEventEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "target_identifier", nullable = false)
    private String targetIdentifier;

    @Column(name = "consumed_minutes", nullable = false)
    private int consumedMinutes;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public String getTargetIdentifier() { return targetIdentifier; }
    public void setTargetIdentifier(String targetIdentifier) { this.targetIdentifier = targetIdentifier; }

    public int getConsumedMinutes() { return consumedMinutes; }
    public void setConsumedMinutes(int consumedMinutes) { this.consumedMinutes = consumedMinutes; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
