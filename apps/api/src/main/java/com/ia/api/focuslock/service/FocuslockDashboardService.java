package com.ia.api.focuslock.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.focuslock.api.FlDashboardResponse;
import com.ia.api.focuslock.api.FlDeviceResponse;
import com.ia.api.focuslock.api.FlInsightsResponse;
import com.ia.api.focuslock.api.FlRuleResponse;
import com.ia.api.focuslock.api.FlUsageEventRequest;
import com.ia.api.focuslock.api.FlUsageItem;
import com.ia.api.focuslock.api.FlWeeklyDayItem;
import com.ia.api.focuslock.domain.FlDeviceEntity;
import com.ia.api.focuslock.domain.FlUsageEventEntity;
import com.ia.api.focuslock.repository.FlDeviceRepository;
import com.ia.api.focuslock.repository.FlUsageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// @Service  // FocusLock: disabled while native app is being built
public class FocuslockDashboardService {

    private final FlDeviceRepository deviceRepository;
    private final FlUsageEventRepository usageEventRepository;
    private final FocuslockRuleService ruleService;
    private final UserRepository userRepository;

    public FocuslockDashboardService(
            FlDeviceRepository deviceRepository,
            FlUsageEventRepository usageEventRepository,
            FocuslockRuleService ruleService,
            UserRepository userRepository
    ) {
        this.deviceRepository = deviceRepository;
        this.usageEventRepository = usageEventRepository;
        this.ruleService = ruleService;
        this.userRepository = userRepository;
    }

    public FlDashboardResponse getDashboard(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        LocalDate today = LocalDate.now();

        List<FlDeviceResponse> devices = deviceRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toDeviceResponse).toList();

        FlDeviceResponse activeDevice = devices.stream()
                .filter(d -> "ACTIVE".equals(d.status()))
                .findFirst().orElse(null);

        List<FlRuleResponse> rules = ruleService.listRules(email);
        List<FlRuleResponse> activeRules = rules.stream().filter(FlRuleResponse::active).toList();

        List<FlUsageItem> todayUsage = usageEventRepository
                .findByUserIdAndEventDate(user.getId(), today)
                .stream()
                .map(e -> new FlUsageItem(e.getTargetIdentifier(), e.getConsumedMinutes()))
                .toList();

        int totalMinutesToday = todayUsage.stream().mapToInt(FlUsageItem::consumedMinutes).sum();

        return new FlDashboardResponse(
                activeDevice,
                activeRules.size(),
                rules.size(),
                totalMinutesToday,
                todayUsage,
                activeRules
        );
    }

    public FlInsightsResponse getInsights(String email) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        List<Object[]> topTargets = usageEventRepository.findTopTargetsByUserAndPeriod(
                user.getId(), weekStart, today);

        List<FlUsageItem> topApps = topTargets.stream()
                .limit(5)
                .map(row -> new FlUsageItem((String) row[0], ((Number) row[1]).intValue()))
                .toList();

        List<Object[]> dailyTotals = usageEventRepository.findDailyTotalsByUserAndPeriod(
                user.getId(), weekStart, today);

        Map<LocalDate, Integer> dailyMap = dailyTotals.stream()
                .collect(Collectors.toMap(
                        row -> (LocalDate) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        List<FlWeeklyDayItem> weeklyBreakdown = new ArrayList<>();
        for (LocalDate d = weekStart; !d.isAfter(today); d = d.plusDays(1)) {
            weeklyBreakdown.add(new FlWeeklyDayItem(
                    d.toString(),
                    d.getDayOfWeek().toString().substring(0, 3),
                    dailyMap.getOrDefault(d, 0)
            ));
        }

        return new FlInsightsResponse(topApps, weeklyBreakdown);
    }

    @Transactional
    public void recordUsage(String email, FlUsageEventRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        UUID deviceId = UUID.fromString(request.deviceId());
        LocalDate eventDate = LocalDate.parse(request.eventDate());

        FlUsageEventEntity existing = usageEventRepository
                .findByUserIdAndDeviceIdAndTargetIdentifierAndEventDate(
                        user.getId(), deviceId, request.targetIdentifier(), eventDate)
                .orElse(null);

        if (existing != null) {
            existing.setConsumedMinutes(request.consumedMinutes());
            usageEventRepository.save(existing);
        } else {
            FlUsageEventEntity event = new FlUsageEventEntity();
            event.setId(UUID.randomUUID());
            event.setUserId(user.getId());
            event.setDeviceId(deviceId);
            event.setTargetIdentifier(request.targetIdentifier());
            event.setConsumedMinutes(request.consumedMinutes());
            event.setEventDate(eventDate);
            event.setCreatedAt(Instant.now());
            usageEventRepository.save(event);
        }
    }

    private FlDeviceResponse toDeviceResponse(FlDeviceEntity d) {
        return new FlDeviceResponse(
                d.getId().toString(),
                d.getDeviceName(),
                d.getStatus(),
                d.isFamilyControlsGranted(),
                d.isScreenTimeGranted(),
                d.isNotificationsGranted(),
                d.getPairedAt() != null ? d.getPairedAt().toString() : null,
                d.getLastSeenAt() != null ? d.getLastSeenAt().toString() : null,
                d.getCreatedAt().toString()
        );
    }
}
