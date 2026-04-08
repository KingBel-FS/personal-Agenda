package com.ia.api.focuslock.repository;

import com.ia.api.focuslock.domain.FlUsageEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlUsageEventRepository extends JpaRepository<FlUsageEventEntity, UUID> {

    List<FlUsageEventEntity> findByUserIdAndEventDate(UUID userId, LocalDate eventDate);

    Optional<FlUsageEventEntity> findByUserIdAndDeviceIdAndTargetIdentifierAndEventDate(
            UUID userId, UUID deviceId, String targetIdentifier, LocalDate eventDate);

    @Query("""
        SELECT u.targetIdentifier, SUM(u.consumedMinutes)
        FROM FlUsageEventEntity u
        WHERE u.userId = :userId AND u.eventDate >= :from AND u.eventDate <= :to
        GROUP BY u.targetIdentifier
        ORDER BY SUM(u.consumedMinutes) DESC
    """)
    List<Object[]> findTopTargetsByUserAndPeriod(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
        SELECT u.eventDate, SUM(u.consumedMinutes)
        FROM FlUsageEventEntity u
        WHERE u.userId = :userId AND u.eventDate >= :from AND u.eventDate <= :to
        GROUP BY u.eventDate
        ORDER BY u.eventDate
    """)
    List<Object[]> findDailyTotalsByUserAndPeriod(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
