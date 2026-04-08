package com.ia.api.today;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StreakSnapshotRepository extends JpaRepository<StreakSnapshotEntity, UUID> {

    Optional<StreakSnapshotEntity> findByUserIdAndSnapshotDate(UUID userId, LocalDate snapshotDate);

    Optional<StreakSnapshotEntity> findTopByUserIdOrderBySnapshotDateDesc(UUID userId);
}
