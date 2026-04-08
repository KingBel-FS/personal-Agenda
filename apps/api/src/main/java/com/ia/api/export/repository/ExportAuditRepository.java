package com.ia.api.export.repository;

import com.ia.api.export.domain.ExportAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExportAuditRepository extends JpaRepository<ExportAuditEntity, UUID> {
    List<ExportAuditEntity> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);
}
