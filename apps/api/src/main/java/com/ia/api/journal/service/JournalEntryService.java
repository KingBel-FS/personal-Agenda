package com.ia.api.journal.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.journal.api.JournalEntryResponse;
import com.ia.api.journal.domain.JournalEntryEntity;
import com.ia.api.journal.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final UserRepository userRepository;

    public JournalEntryService(
            JournalEntryRepository journalEntryRepository,
            UserRepository userRepository
    ) {
        this.journalEntryRepository = journalEntryRepository;
        this.userRepository = userRepository;
    }

    public Optional<JournalEntryResponse> getEntry(String email, LocalDate date) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        validateDateAccessible(date, user);
        return journalEntryRepository.findByUserIdAndEntryDate(user.getId(), date)
                .map(this::toResponse);
    }

    @Transactional
    public JournalEntryResponse upsertEntry(String email, LocalDate date, String content) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        validateDateAccessible(date, user);

        Instant now = Instant.now();
        JournalEntryEntity entity = journalEntryRepository
                .findByUserIdAndEntryDate(user.getId(), date)
                .orElseGet(() -> {
                    JournalEntryEntity e = new JournalEntryEntity();
                    e.setId(UUID.randomUUID());
                    e.setUserId(user.getId());
                    e.setEntryDate(date);
                    e.setCreatedAt(now);
                    return e;
                });
        entity.setContent(content);
        entity.setUpdatedAt(now);
        journalEntryRepository.save(entity);

        return toResponse(entity);
    }

    @Transactional
    public void deleteEntry(String email, LocalDate date) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        validateDateAccessible(date, user);
        journalEntryRepository.deleteByUserIdAndEntryDate(user.getId(), date);
    }

    private void validateDateAccessible(LocalDate date, UserEntity user) {
        ZoneId zone = user.getTimezoneName() != null
                ? ZoneId.of(user.getTimezoneName())
                : ZoneId.of("Europe/Paris");
        LocalDate accountCreationDate = user.getCreatedAt().atZone(zone).toLocalDate();
        if (date.isBefore(accountCreationDate)) {
            throw new IllegalArgumentException("Aucune entrée possible avant la date de création du compte.");
        }
    }

    private JournalEntryResponse toResponse(JournalEntryEntity e) {
        return new JournalEntryResponse(
                e.getEntryDate().toString(),
                e.getContent(),
                e.getCreatedAt().toString(),
                e.getUpdatedAt().toString()
        );
    }
}
