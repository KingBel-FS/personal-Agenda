package com.ia.api.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consents")
public class ConsentEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "consent_type", nullable = false)
    private String consentType;
    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;
    @Column(name = "legal_version", nullable = false)
    private String legalVersion;
    @Column(name = "ip_hash", nullable = false)
    private String ipHash;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getConsentType() { return consentType; }
    public void setConsentType(String consentType) { this.consentType = consentType; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public String getLegalVersion() { return legalVersion; }
    public void setLegalVersion(String legalVersion) { this.legalVersion = legalVersion; }
    public String getIpHash() { return ipHash; }
    public void setIpHash(String ipHash) { this.ipHash = ipHash; }
}
