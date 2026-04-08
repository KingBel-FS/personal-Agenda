package com.ia.api.user.repository;

import com.ia.api.user.domain.DayProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DayProfileRepository extends JpaRepository<DayProfileEntity, UUID> {
    List<DayProfileEntity> findAllByUserId(UUID userId);
    void deleteAllByUserId(UUID userId);
}
