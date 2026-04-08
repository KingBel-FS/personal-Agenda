package com.ia.api.auth.repository;

import com.ia.api.auth.domain.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
    List<RefreshTokenEntity> findAllByUserIdAndRevokedAtIsNull(UUID userId);
    void deleteAllByUserId(UUID userId);
}
