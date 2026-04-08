package com.ia.api.wakeup.repository;

import com.ia.api.wakeup.domain.WakeUpOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface WakeUpOverrideRepository extends JpaRepository<WakeUpOverrideEntity, UUID> {
    Optional<WakeUpOverrideEntity> findByUserIdAndOverrideDate(UUID userId, LocalDate overrideDate);
    void deleteByUserIdAndOverrideDate(UUID userId, LocalDate overrideDate);
}
