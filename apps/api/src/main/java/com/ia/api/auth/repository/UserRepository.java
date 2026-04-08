package com.ia.api.auth.repository;

import com.ia.api.auth.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<UserEntity> findByEmailIgnoreCase(String email);
}
