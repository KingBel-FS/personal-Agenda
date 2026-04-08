package com.ia.api.user.repository;

import com.ia.api.user.domain.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {
    Optional<AssetEntity> findFirstByUserIdAndAssetTypeOrderByCreatedAtDesc(UUID userId, String assetType);
    List<AssetEntity> findAllByUserId(UUID userId);
    void deleteAllByUserId(UUID userId);
}
