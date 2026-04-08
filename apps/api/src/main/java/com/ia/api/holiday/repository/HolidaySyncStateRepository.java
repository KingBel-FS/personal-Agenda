package com.ia.api.holiday.repository;

import com.ia.api.holiday.domain.HolidaySyncStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HolidaySyncStateRepository extends JpaRepository<HolidaySyncStateEntity, UUID> {
    Optional<HolidaySyncStateEntity> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
