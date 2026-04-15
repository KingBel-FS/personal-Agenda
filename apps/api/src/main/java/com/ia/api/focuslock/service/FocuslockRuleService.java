package com.ia.api.focuslock.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.focuslock.api.FlRuleRequest;
import com.ia.api.focuslock.api.FlRuleResponse;
import com.ia.api.focuslock.api.FlScheduleItem;
import com.ia.api.focuslock.api.FlWebDomainItem;
import com.ia.api.focuslock.domain.FlRuleEntity;
import com.ia.api.focuslock.domain.FlScheduleEntity;
import com.ia.api.focuslock.domain.FlWebDomainEntity;
import com.ia.api.focuslock.repository.FlOverrideEventRepository;
import com.ia.api.focuslock.repository.FlRuleRepository;
import com.ia.api.focuslock.repository.FlScheduleRepository;
import com.ia.api.focuslock.repository.FlWebDomainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

// @Service  // FocusLock: disabled while native app is being built
public class FocuslockRuleService {

    private final FlRuleRepository ruleRepository;
    private final FlScheduleRepository scheduleRepository;
    private final FlWebDomainRepository webDomainRepository;
    private final FlOverrideEventRepository overrideEventRepository;
    private final UserRepository userRepository;

    public FocuslockRuleService(
            FlRuleRepository ruleRepository,
            FlScheduleRepository scheduleRepository,
            FlWebDomainRepository webDomainRepository,
            FlOverrideEventRepository overrideEventRepository,
            UserRepository userRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.scheduleRepository = scheduleRepository;
        this.webDomainRepository = webDomainRepository;
        this.overrideEventRepository = overrideEventRepository;
        this.userRepository = userRepository;
    }

    public List<FlRuleResponse> listRules(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return ruleRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public FlRuleResponse createRule(String email, FlRuleRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        validateRequest(request);

        Instant now = Instant.now();
        FlRuleEntity rule = new FlRuleEntity();
        rule.setId(UUID.randomUUID());
        rule.setUserId(user.getId());
        rule.setName(request.name());
        rule.setTargetType(request.targetType());
        rule.setTargetIdentifier(request.targetIdentifier());
        rule.setRuleType(request.ruleType());
        rule.setLimitMinutes(request.limitMinutes());
        rule.setFrictionType(request.frictionType() != null ? request.frictionType() : "NONE");
        rule.setActive(true);
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        ruleRepository.save(rule);

        saveSchedules(rule.getId(), request.schedules());
        saveDomains(rule.getId(), request.domains());

        return toResponse(rule);
    }

    @Transactional
    public FlRuleResponse updateRule(String email, UUID ruleId, FlRuleRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        FlRuleEntity rule = ruleRepository.findByIdAndUserId(ruleId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Règle introuvable."));
        validateRequest(request);

        rule.setName(request.name());
        rule.setTargetType(request.targetType());
        rule.setTargetIdentifier(request.targetIdentifier());
        rule.setRuleType(request.ruleType());
        rule.setLimitMinutes(request.limitMinutes());
        rule.setFrictionType(request.frictionType() != null ? request.frictionType() : "NONE");
        rule.setUpdatedAt(Instant.now());
        ruleRepository.save(rule);

        scheduleRepository.deleteByRuleId(ruleId);
        webDomainRepository.deleteByRuleId(ruleId);
        saveSchedules(ruleId, request.schedules());
        saveDomains(ruleId, request.domains());

        return toResponse(rule);
    }

    @Transactional
    public FlRuleResponse toggleRule(String email, UUID ruleId) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        FlRuleEntity rule = ruleRepository.findByIdAndUserId(ruleId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Règle introuvable."));
        rule.setActive(!rule.isActive());
        rule.setUpdatedAt(Instant.now());
        ruleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public void deleteRule(String email, UUID ruleId) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        FlRuleEntity rule = ruleRepository.findByIdAndUserId(ruleId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Règle introuvable."));
        scheduleRepository.deleteByRuleId(ruleId);
        webDomainRepository.deleteByRuleId(ruleId);
        ruleRepository.delete(rule);
    }

    private void saveSchedules(UUID ruleId, List<FlScheduleItem> schedules) {
        if (schedules == null || schedules.isEmpty()) return;
        for (FlScheduleItem item : schedules) {
            FlScheduleEntity s = new FlScheduleEntity();
            s.setId(UUID.randomUUID());
            s.setRuleId(ruleId);
            s.setStartTime(LocalTime.parse(item.startTime()));
            s.setEndTime(LocalTime.parse(item.endTime()));
            s.setDaysOfWeek(item.daysOfWeek());
            scheduleRepository.save(s);
        }
    }

    private void saveDomains(UUID ruleId, List<String> domains) {
        if (domains == null || domains.isEmpty()) return;
        Instant now = Instant.now();
        for (String domain : domains) {
            if (domain == null || domain.isBlank()) continue;
            FlWebDomainEntity d = new FlWebDomainEntity();
            d.setId(UUID.randomUUID());
            d.setRuleId(ruleId);
            d.setDomain(domain.trim().toLowerCase());
            d.setCreatedAt(now);
            webDomainRepository.save(d);
        }
    }

    private void validateRequest(FlRuleRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Le nom de la règle est obligatoire.");
        }
        if (!"APP".equals(request.targetType()) && !"CATEGORY".equals(request.targetType()) && !"DOMAIN".equals(request.targetType())) {
            throw new IllegalArgumentException("Type de cible invalide : APP, CATEGORY ou DOMAIN.");
        }
        if (!"DAILY_LIMIT".equals(request.ruleType()) && !"TIME_BLOCK".equals(request.ruleType())) {
            throw new IllegalArgumentException("Type de règle invalide : DAILY_LIMIT ou TIME_BLOCK.");
        }
        if ("DAILY_LIMIT".equals(request.ruleType()) && (request.limitMinutes() == null || request.limitMinutes() < 1)) {
            throw new IllegalArgumentException("Une limite quotidienne doit avoir une durée positive.");
        }
    }

    private FlRuleResponse toResponse(FlRuleEntity rule) {
        List<FlScheduleItem> schedules = scheduleRepository.findByRuleId(rule.getId())
                .stream()
                .map(s -> new FlScheduleItem(
                        s.getId().toString(),
                        s.getStartTime().toString(),
                        s.getEndTime().toString(),
                        s.getDaysOfWeek()))
                .toList();

        List<FlWebDomainItem> domains = webDomainRepository.findByRuleId(rule.getId())
                .stream()
                .map(d -> new FlWebDomainItem(d.getId().toString(), d.getDomain()))
                .toList();

        long overrideCount = overrideEventRepository.countByRuleId(rule.getId());

        return new FlRuleResponse(
                rule.getId().toString(),
                rule.getName(),
                rule.getTargetType(),
                rule.getTargetIdentifier(),
                rule.getRuleType(),
                rule.getLimitMinutes(),
                rule.getFrictionType(),
                rule.isActive(),
                schedules,
                domains,
                overrideCount,
                rule.getCreatedAt().toString(),
                rule.getUpdatedAt().toString()
        );
    }
}
