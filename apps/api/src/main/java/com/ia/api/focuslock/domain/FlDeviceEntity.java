package com.ia.api.focuslock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fl_device")
public class FlDeviceEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "pairing_token")
    private String pairingToken;

    @Column(name = "pairing_token_expires_at")
    private Instant pairingTokenExpiresAt;

    @Column(name = "status", nullable = false)
    private String status; // PENDING, ACTIVE, REVOKED

    @Column(name = "family_controls_granted", nullable = false)
    private boolean familyControlsGranted;

    @Column(name = "screen_time_granted", nullable = false)
    private boolean screenTimeGranted;

    @Column(name = "notifications_granted", nullable = false)
    private boolean notificationsGranted;

    @Column(name = "paired_at")
    private Instant pairedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getPairingToken() { return pairingToken; }
    public void setPairingToken(String pairingToken) { this.pairingToken = pairingToken; }

    public Instant getPairingTokenExpiresAt() { return pairingTokenExpiresAt; }
    public void setPairingTokenExpiresAt(Instant pairingTokenExpiresAt) { this.pairingTokenExpiresAt = pairingTokenExpiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isFamilyControlsGranted() { return familyControlsGranted; }
    public void setFamilyControlsGranted(boolean familyControlsGranted) { this.familyControlsGranted = familyControlsGranted; }

    public boolean isScreenTimeGranted() { return screenTimeGranted; }
    public void setScreenTimeGranted(boolean screenTimeGranted) { this.screenTimeGranted = screenTimeGranted; }

    public boolean isNotificationsGranted() { return notificationsGranted; }
    public void setNotificationsGranted(boolean notificationsGranted) { this.notificationsGranted = notificationsGranted; }

    public Instant getPairedAt() { return pairedAt; }
    public void setPairedAt(Instant pairedAt) { this.pairedAt = pairedAt; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
