package com.ia.api.holiday.service;

import com.ia.api.holiday.domain.HolidaySyncStateEntity;
import com.ia.api.holiday.domain.HolidaySyncStatus;
import com.ia.api.holiday.repository.HolidaySyncStateRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class HolidaySyncStateService {
    private final HolidaySyncStateRepository holidaySyncStateRepository;

    public HolidaySyncStateService(HolidaySyncStateRepository holidaySyncStateRepository) {
        this.holidaySyncStateRepository = holidaySyncStateRepository;
    }

    public void markPending(UUID userId, String geographicZone) {
        HolidaySyncStateEntity state = holidaySyncStateRepository.findByUserId(userId)
                .orElseGet(HolidaySyncStateEntity::new);
        if (state.getId() == null) {
            state.setId(UUID.randomUUID());
            state.setUserId(userId);
        }
        state.setGeographicZone(geographicZone);
        state.setStatus(HolidaySyncStatus.PENDING);
        state.setLastError(null);
        state.setNextRetryAt(null);
        state.setRetryCount(0);
        state.setUpdatedAt(Instant.now());
        holidaySyncStateRepository.save(state);
    }
}
