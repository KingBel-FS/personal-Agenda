package com.ia.api.journal.repository;

import com.ia.api.journal.domain.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, UUID> {
    Optional<JournalEntryEntity> findByUserIdAndEntryDate(UUID userId, LocalDate entryDate);
    void deleteByUserIdAndEntryDate(UUID userId, LocalDate entryDate);
}
