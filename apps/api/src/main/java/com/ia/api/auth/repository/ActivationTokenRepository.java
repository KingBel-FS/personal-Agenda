package com.ia.api.auth.repository;

import com.ia.api.auth.domain.ActivationTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivationTokenRepository extends JpaRepository<ActivationTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ActivationTokenEntity a WHERE a.tokenHash = :tokenHash")
    Optional<ActivationTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Query("SELECT a FROM ActivationTokenEntity a WHERE a.userId = :userId AND a.usedAt IS NULL AND a.expiresAt > :now")
    List<ActivationTokenEntity> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    void deleteAllByUserId(UUID userId);
}
