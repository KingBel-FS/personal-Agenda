package com.ia.api.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_jobs")
public class NotificationJobEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "occurrence_id", nullable = false)
    private UUID occurrenceId;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getOccurrenceId() { return occurrenceId; }
    public String getTriggerType() { return triggerType; }
    public Instant getScheduledAt() { return scheduledAt; }
    public String getStatus() { return status; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCanceledAt() { return canceledAt; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public void setCanceledAt(Instant canceledAt) { this.canceledAt = canceledAt; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
