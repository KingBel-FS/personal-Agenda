package com.ia.api.today;

import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import com.ia.api.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private TaskOccurrenceRepository occurrenceRepository;
    @Mock
    private StreakSnapshotRepository snapshotRepository;
    @Mock
    private BadgeRepository badgeRepository;

    private StreakService streakService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        streakService = new StreakService(occurrenceRepository, snapshotRepository, badgeRepository);
        when(snapshotRepository.findTopByUserIdOrderBySnapshotDateDesc(userId)).thenReturn(Optional.empty());
        when(snapshotRepository.findByUserIdAndSnapshotDate(eq(userId), any())).thenReturn(Optional.empty());
        lenient().when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(badgeRepository.existsByUserIdAndBadgeType(any(), any())).thenReturn(false);
        lenient().when(badgeRepository.findAllByUserId(userId)).thenReturn(List.of());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TaskOccurrenceEntity occ(LocalDate date, String status) {
        var o = new TaskOccurrenceEntity();
        o.setId(UUID.randomUUID());
        o.setUserId(userId);
        o.setTaskDefinitionId(UUID.randomUUID());
        o.setTaskRuleId(UUID.randomUUID());
        o.setOccurrenceDate(date);
        o.setOccurrenceTime(LocalTime.of(8, 0));
        o.setStatus(status);
        o.setDayCategory("NORMAL");
        return o;
    }

    private void givenOccurrences(List<TaskOccurrenceEntity> occs) {
        when(occurrenceRepository
                .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                        eq(userId), eq("canceled"), any(), any()))
                .thenReturn(occs);
    }

    // -------------------------------------------------------------------------
    // Tests du comportement corrigé : jours vides cassent la streak immédiatement
    // -------------------------------------------------------------------------

    @Test
    void jour_vide_hier_casse_la_streak() {
        // Scenario : aujourd'hui planned, hier aucune occurrence, avant-hier done
        // Résultat attendu : streak = 0 (hier vide => break immédiat)
        LocalDate today = LocalDate.now(TaskService.PARIS);
        LocalDate twoDaysAgo = today.minusDays(2);

        givenOccurrences(List.of(
                occ(today, "planned"),
                occ(twoDaysAgo, "done")
                // yesterday has no occurrence at all
        ));

        var result = streakService.recalculate(userId);

        assertThat(result.currentStreak()).isEqualTo(0);
        assertThat(result.streakActive()).isFalse();
    }

    @Test
    void cinq_jours_inactivite_casse_la_streak() {
        // Scenario : dernière occurrence "done" il y a 5 jours, rien depuis
        // Résultat attendu : streak = 0 (jours 1-4 vides => break au premier)
        LocalDate today = LocalDate.now(TaskService.PARIS);
        LocalDate fiveDaysAgo = today.minusDays(5);

        givenOccurrences(List.of(
                occ(today, "planned"),
                occ(fiveDaysAgo, "done")
                // days -1 to -4 have no occurrences
        ));

        var result = streakService.recalculate(userId);

        assertThat(result.currentStreak()).isEqualTo(0);
        assertThat(result.streakActive()).isFalse();
    }

    @Test
    void aucune_occurrence_du_tout_donne_streak_zero() {
        givenOccurrences(List.of());

        var result = streakService.recalculate(userId);

        assertThat(result.currentStreak()).isEqualTo(0);
        assertThat(result.streakActive()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Comportements nominaux maintenus
    // -------------------------------------------------------------------------

    @Test
    void jours_consecutifs_done_maintiennent_la_streak() {
        LocalDate today = LocalDate.now(TaskService.PARIS);

        givenOccurrences(List.of(
                occ(today, "planned"),
                occ(today.minusDays(1), "done"),
                occ(today.minusDays(2), "done"),
                occ(today.minusDays(3), "done")
        ));

        var result = streakService.recalculate(userId);

        assertThat(result.currentStreak()).isEqualTo(3);
        assertThat(result.streakActive()).isTrue();
    }

    @Test
    void jour_transparent_skipped_ne_casse_pas_la_streak() {
        // skipped/canceled = transparent → ne casse pas, ne compte pas
        LocalDate today = LocalDate.now(TaskService.PARIS);

        givenOccurrences(List.of(
                occ(today, "planned"),
                occ(today.minusDays(1), "done"),
                occ(today.minusDays(2), "skipped"),   // transparent
                occ(today.minusDays(3), "done"),
                occ(today.minusDays(4), "done")
                // day -5 is empty → break after counting -1, -3, -4
        ));

        var result = streakService.recalculate(userId);

        // -1 done, -2 transparent (skip), -3 done, -4 done => streak = 3
        assertThat(result.currentStreak()).isEqualTo(3);
        assertThat(result.streakActive()).isTrue();
    }

    @Test
    void suspension_valide_compte_comme_ok() {
        LocalDate today = LocalDate.now(TaskService.PARIS);

        givenOccurrences(List.of(
                occ(today, "planned"),
                occ(today.minusDays(1), "suspended"),
                occ(today.minusDays(2), "done")
        ));

        var result = streakService.recalculate(userId);

        assertThat(result.currentStreak()).isEqualTo(2);
        assertThat(result.streakActive()).isTrue();
    }

    @Test
    void tache_missed_hier_casse_la_streak() {
        LocalDate today = LocalDate.now(TaskService.PARIS);

        givenOccurrences(List.of(
                occ(today, "planned"),
                occ(today.minusDays(1), "missed"),
                occ(today.minusDays(2), "done")
        ));

        var result = streakService.recalculate(userId);

        assertThat(result.currentStreak()).isEqualTo(0);
        assertThat(result.streakActive()).isFalse();
    }

    @Test
    void toutes_taches_aujourdhui_done_comptent_dans_streak() {
        LocalDate today = LocalDate.now(TaskService.PARIS);

        givenOccurrences(List.of(
                occ(today, "done"),
                occ(today.minusDays(1), "done")
        ));

        var result = streakService.recalculate(userId);

        assertThat(result.currentStreak()).isEqualTo(2);
        assertThat(result.streakActive()).isTrue();
    }

    @Test
    void streak_interrompue_par_tache_planned_dans_le_passe() {
        // "planned" dans le passé = oublié = casse la streak
        LocalDate today = LocalDate.now(TaskService.PARIS);

        givenOccurrences(List.of(
                occ(today, "planned"),
                occ(today.minusDays(1), "done"),
                occ(today.minusDays(2), "planned"),  // passé non fait
                occ(today.minusDays(3), "done")
        ));

        var result = streakService.recalculate(userId);

        // -1 done (streak=1), -2 planned passé => break, streakActive = true (streak>0)
        assertThat(result.currentStreak()).isEqualTo(1);
        assertThat(result.streakActive()).isTrue();
    }
}
