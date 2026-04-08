package com.ia.api.holiday.repository;

import com.ia.api.holiday.domain.HolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HolidayRepository extends JpaRepository<HolidayEntity, UUID> {
    List<HolidayEntity> findAllByGeographicZoneOrderByHolidayDateAsc(String geographicZone);
}
