package com.ia.api.today;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OccurrenceStatusEventRepository extends JpaRepository<OccurrenceStatusEventEntity, UUID> {

    List<OccurrenceStatusEventEntity> findAllByOccurrenceIdOrderByOccurredAtDesc(UUID occurrenceId);
}
