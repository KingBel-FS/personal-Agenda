package com.ia.api.vacation.repository;

import com.ia.api.vacation.domain.VacationPeriodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VacationPeriodRepository extends JpaRepository<VacationPeriodEntity, UUID> {
    List<VacationPeriodEntity> findAllByUserIdOrderByStartDateAsc(UUID userId);
    void deleteAllByUserId(UUID userId);

    @Query("SELECT v FROM VacationPeriodEntity v WHERE v.userId = :userId AND v.startDate <= :endDate AND v.endDate >= :startDate AND (:excludeId IS NULL OR v.id <> :excludeId)")
    List<VacationPeriodEntity> findOverlapping(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") UUID excludeId
    );
}
