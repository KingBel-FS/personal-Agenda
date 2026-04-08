package com.ia.api.auth.repository;

import com.ia.api.auth.domain.ConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsentRepository extends JpaRepository<ConsentEntity, UUID> {
    void deleteAllByUserId(UUID userId);
}
