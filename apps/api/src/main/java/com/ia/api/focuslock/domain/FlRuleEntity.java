package com.ia.api.focuslock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fl_rule")
public class FlRuleEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "target_type", nullable = false)
    private String targetType; // APP, CATEGORY, DOMAIN

    @Column(name = "target_identifier", nullable = false)
    private String targetIdentifier;

    @Column(name = "rule_type", nullable = false)
    private String ruleType; // DAILY_LIMIT, TIME_BLOCK

    @Column(name = "limit_minutes")
    private Integer limitMinutes;

    @Column(name = "friction_type", nullable = false)
    private String frictionType; // NONE, DELAY_60, CONFIRMATION

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetIdentifier() { return targetIdentifier; }
    public void setTargetIdentifier(String targetIdentifier) { this.targetIdentifier = targetIdentifier; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public Integer getLimitMinutes() { return limitMinutes; }
    public void setLimitMinutes(Integer limitMinutes) { this.limitMinutes = limitMinutes; }

    public String getFrictionType() { return frictionType; }
    public void setFrictionType(String frictionType) { this.frictionType = frictionType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
