package com.ia.api.focuslock.repository;

import com.ia.api.focuslock.domain.FlWebDomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FlWebDomainRepository extends JpaRepository<FlWebDomainEntity, UUID> {
    List<FlWebDomainEntity> findByRuleId(UUID ruleId);
    void deleteByRuleId(UUID ruleId);
}
