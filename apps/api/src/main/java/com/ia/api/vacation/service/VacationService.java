package com.ia.api.vacation.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.user.api.VacationPeriodRequest;
import com.ia.api.user.api.VacationPeriodResponse;
import com.ia.api.vacation.domain.VacationPeriodEntity;
import com.ia.api.vacation.repository.VacationPeriodRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VacationService {
    private final UserRepository userRepository;
    private final VacationPeriodRepository vacationPeriodRepository;

    public VacationService(UserRepository userRepository, VacationPeriodRepository vacationPeriodRepository) {
        this.userRepository = userRepository;
        this.vacationPeriodRepository = vacationPeriodRepository;
    }

    @Transactional
    public List<VacationPeriodResponse> list(String email) {
        UserEntity user = getUser(email);
        return vacationPeriodRepository.findAllByUserIdOrderByStartDateAsc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VacationPeriodResponse create(String email, VacationPeriodRequest request) {
        UserEntity user = getUser(email);
        validateDates(request);
        validateNoOverlap(user.getId(), request, null);

        VacationPeriodEntity entity = new VacationPeriodEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(user.getId());
        entity.setLabel(request.label().trim());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        vacationPeriodRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public VacationPeriodResponse update(String email, UUID vacationId, VacationPeriodRequest request) {
        UserEntity user = getUser(email);
        validateDates(request);
        validateNoOverlap(user.getId(), request, vacationId);

        VacationPeriodEntity entity = vacationPeriodRepository.findById(vacationId)
                .filter(item -> item.getUserId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Période de vacances introuvable"));
        entity.setLabel(request.label().trim());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setUpdatedAt(Instant.now());
        return toResponse(entity);
    }

    @Transactional
    public void delete(String email, UUID vacationId) {
        UserEntity user = getUser(email);
        VacationPeriodEntity entity = vacationPeriodRepository.findById(vacationId)
                .filter(item -> item.getUserId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Période de vacances introuvable"));
        vacationPeriodRepository.delete(entity);
    }

    private UserEntity getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    private void validateDates(VacationPeriodRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("La date de fin des vacances doit être égale ou postérieure à la date de début");
        }
    }

    private void validateNoOverlap(UUID userId, VacationPeriodRequest request, UUID excludeId) {
        List<VacationPeriodEntity> overlapping = vacationPeriodRepository.findOverlapping(
                userId, request.startDate(), request.endDate(), excludeId);
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("La période de vacances chevauche une période existante");
        }
    }

    private VacationPeriodResponse toResponse(VacationPeriodEntity entity) {
        return new VacationPeriodResponse(entity.getId(), entity.getLabel(), entity.getStartDate(), entity.getEndDate());
    }
}
