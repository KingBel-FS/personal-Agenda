package com.ia.api.focuslock.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.focuslock.api.FlDevicePermissionsRequest;
import com.ia.api.focuslock.api.FlDeviceResponse;
import com.ia.api.focuslock.api.FlPairingTokenResponse;
import com.ia.api.focuslock.domain.FlDeviceEntity;
import com.ia.api.focuslock.repository.FlDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

// @Service  // FocusLock: disabled while native app is being built
public class FocuslockDeviceService {

    private final FlDeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public FocuslockDeviceService(FlDeviceRepository deviceRepository, UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    public List<FlDeviceResponse> listDevices(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return deviceRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public FlPairingTokenResponse generatePairingToken(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        // Invalidate any existing PENDING device for this user
        deviceRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .forEach(d -> {
                    d.setStatus("REVOKED");
                    deviceRepository.save(d);
                });

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        FlDeviceEntity device = new FlDeviceEntity();
        device.setId(UUID.randomUUID());
        device.setUserId(user.getId());
        device.setDeviceName("iPhone");
        device.setPairingToken(token);
        device.setPairingTokenExpiresAt(expiresAt);
        device.setStatus("PENDING");
        device.setCreatedAt(Instant.now());
        deviceRepository.save(device);

        return new FlPairingTokenResponse(token, expiresAt.toString());
    }

    @Transactional
    public FlDeviceResponse confirmPairing(String token, String deviceName) {
        FlDeviceEntity device = deviceRepository.findByPairingToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide ou expiré."));
        if (!"PENDING".equals(device.getStatus())) {
            throw new IllegalArgumentException("Ce token a déjà été utilisé.");
        }
        if (Instant.now().isAfter(device.getPairingTokenExpiresAt())) {
            device.setStatus("REVOKED");
            deviceRepository.save(device);
            throw new IllegalArgumentException("Le token a expiré.");
        }
        device.setStatus("ACTIVE");
        device.setDeviceName(deviceName != null && !deviceName.isBlank() ? deviceName : "iPhone");
        device.setPairedAt(Instant.now());
        device.setLastSeenAt(Instant.now());
        device.setPairingToken(null);
        device.setPairingTokenExpiresAt(null);
        deviceRepository.save(device);
        return toResponse(device);
    }

    @Transactional
    public void revokeDevice(String email, UUID deviceId) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        FlDeviceEntity device = deviceRepository.findById(deviceId)
                .filter(d -> d.getUserId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Appareil introuvable."));
        device.setStatus("REVOKED");
        deviceRepository.save(device);
    }

    @Transactional
    public FlDeviceResponse updatePermissions(UUID deviceId, FlDevicePermissionsRequest request) {
        FlDeviceEntity device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Appareil introuvable."));
        device.setFamilyControlsGranted(request.familyControlsGranted());
        device.setScreenTimeGranted(request.screenTimeGranted());
        device.setNotificationsGranted(request.notificationsGranted());
        device.setLastSeenAt(Instant.now());
        deviceRepository.save(device);
        return toResponse(device);
    }

    private FlDeviceResponse toResponse(FlDeviceEntity d) {
        return new FlDeviceResponse(
                d.getId().toString(),
                d.getDeviceName(),
                d.getStatus(),
                d.isFamilyControlsGranted(),
                d.isScreenTimeGranted(),
                d.isNotificationsGranted(),
                d.getPairedAt() != null ? d.getPairedAt().toString() : null,
                d.getLastSeenAt() != null ? d.getLastSeenAt().toString() : null,
                d.getCreatedAt().toString()
        );
    }
}
