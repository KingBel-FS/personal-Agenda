package com.ia.api.auth.repository;

import com.ia.api.auth.domain.ResetPasswordTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResetPasswordTokenRepository extends JpaRepository<ResetPasswordTokenEntity, UUID> {
    Optional<ResetPasswordTokenEntity> findByTokenHash(String tokenHash);
    List<ResetPasswordTokenEntity> findAllByUserIdAndUsedAtIsNull(UUID userId);
    void deleteAllByUserId(UUID userId);
}
