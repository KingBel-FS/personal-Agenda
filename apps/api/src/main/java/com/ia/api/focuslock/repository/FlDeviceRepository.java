package com.ia.api.focuslock.repository;

import com.ia.api.focuslock.domain.FlDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlDeviceRepository extends JpaRepository<FlDeviceEntity, UUID> {
    List<FlDeviceEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<FlDeviceEntity> findByPairingToken(String pairingToken);
}
