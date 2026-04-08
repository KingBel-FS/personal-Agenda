package com.ia.api.focuslock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fl_override_event")
public class FlOverrideEventEntity {

    @Id
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "friction_applied", nullable = false)
    private String frictionApplied;

    @Column(name = "overridden_at", nullable = false)
    private Instant overriddenAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRuleId() { return ruleId; }
    public void setRuleId(UUID ruleId) { this.ruleId = ruleId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getFrictionApplied() { return frictionApplied; }
    public void setFrictionApplied(String frictionApplied) { this.frictionApplied = frictionApplied; }

    public Instant getOverriddenAt() { return overriddenAt; }
    public void setOverriddenAt(Instant overriddenAt) { this.overriddenAt = overriddenAt; }
}
