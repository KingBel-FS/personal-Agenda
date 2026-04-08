package com.ia.api.export.service;

import com.ia.api.auth.domain.AccountStatus;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.export.api.ExportRequest;
import com.ia.api.export.domain.ExportAuditEntity;
import com.ia.api.export.repository.ExportAuditRepository;
import com.ia.api.goal.domain.GoalEntity;
import com.ia.api.goal.repository.GoalRepository;
import com.ia.api.task.domain.TaskDefinitionEntity;
import com.ia.api.task.domain.TaskOccurrenceEntity;
import com.ia.api.task.repository.TaskDefinitionRepository;
import com.ia.api.task.repository.TaskOccurrenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TaskDefinitionRepository taskDefinitionRepository;
    @Mock private TaskOccurrenceRepository taskOccurrenceRepository;
    @Mock private GoalRepository goalRepository;
    @Mock private ExportAuditRepository exportAuditRepository;

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService(
                userRepository,
                taskDefinitionRepository,
                taskOccurrenceRepository,
                goalRepository,
                exportAuditRepository
        );
        when(exportAuditRepository.save(any(ExportAuditEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // Excel tests
    // -------------------------------------------------------------------------

    @Test
    void generatesExcelAndPersistsSuccessAudit() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(taskDefinition()));
        when(taskOccurrenceRepository
                .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                        user.getId(), "canceled", LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 27)))
                .thenReturn(List.of(occurrence(user.getId(), "done")));
        when(goalRepository.findAllByUserId(user.getId())).thenReturn(List.of());

        ExportService.ExportPayload payload = exportService.generateExport(
                "alice@example.com",
                new ExportRequest("EXCEL", "FULL", "2026-03-20", "2026-03-27")
        );

        assertThat(payload.fileName()).endsWith(".xlsx");
        assertThat(payload.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // Valid XLSX starts with PK (zip magic bytes)
        assertThat(payload.content()[0]).isEqualTo((byte) 0x50);
        assertThat(payload.content()[1]).isEqualTo((byte) 0x4B);

        ArgumentCaptor<ExportAuditEntity> captor = ArgumentCaptor.forClass(ExportAuditEntity.class);
        verify(exportAuditRepository, atLeast(2)).save(captor.capture());
        ExportAuditEntity latest = captor.getAllValues().getLast();
        assertThat(latest.getStatus()).isEqualTo("SUCCESS");
        assertThat(latest.getRowCount()).isEqualTo(2); // 1 def + 1 occurrence
        assertThat(latest.getFileName()).endsWith(".xlsx");
    }

    @Test
    void excelScopeTasksOnlySkipsHistory() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(taskDefinition()));
        when(goalRepository.findAllByUserId(user.getId())).thenReturn(List.of());

        ExportService.ExportPayload payload = exportService.generateExport(
                "alice@example.com",
                new ExportRequest("EXCEL", "TASKS", "2026-03-20", "2026-03-27")
        );

        assertThat(payload.fileName()).contains("tasks").endsWith(".xlsx");
        assertThat(payload.content().length).isGreaterThan(0);
    }

    // -------------------------------------------------------------------------
    // PDF tests
    // -------------------------------------------------------------------------

    @Test
    void generatesPdfWithOccurrencesAndPersistsAudit() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(taskDefinition()));
        when(taskOccurrenceRepository
                .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                        user.getId(), "canceled", LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 27)))
                .thenReturn(List.of(
                        occurrence(user.getId(), "done"),
                        occurrence(user.getId(), "missed")
                ));
        when(goalRepository.findAllByUserId(user.getId())).thenReturn(List.of());

        ExportService.ExportPayload payload = exportService.generateExport(
                "alice@example.com",
                new ExportRequest("PDF", "FULL", "2026-03-20", "2026-03-27")
        );

        assertThat(payload.fileName()).endsWith(".pdf");
        assertThat(payload.contentType()).isEqualTo("application/pdf");
        // PDF magic bytes
        assertThat(new String(payload.content(), 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generatesPdfWithNoHistoryStillProducesPdf() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(taskDefinition()));
        when(taskOccurrenceRepository
                .findAllByUserIdAndStatusNotAndOccurrenceDateBetweenOrderByOccurrenceDateAscOccurrenceTimeAsc(
                        any(), any(), any(), any()))
                .thenReturn(List.of());
        when(goalRepository.findAllByUserId(user.getId())).thenReturn(List.of());

        ExportService.ExportPayload payload = exportService.generateExport(
                "alice@example.com",
                new ExportRequest("PDF", "FULL", "2026-03-20", "2026-03-27")
        );

        assertThat(payload.content().length).isGreaterThan(0);
        assertThat(new String(payload.content(), 0, 4)).isEqualTo("%PDF");
    }

    // -------------------------------------------------------------------------
    // Validation / edge cases
    // -------------------------------------------------------------------------

    @Test
    void auditMarkedFailedOnException() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(taskDefinitionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenThrow(new RuntimeException("DB error"));
        lenient().when(goalRepository.findAllByUserId(any())).thenReturn(List.of());

        try {
            exportService.generateExport("alice@example.com",
                    new ExportRequest("EXCEL", "TASKS", "2026-03-20", "2026-03-27"));
        } catch (RuntimeException ignored) {
        }

        ArgumentCaptor<ExportAuditEntity> captor = ArgumentCaptor.forClass(ExportAuditEntity.class);
        verify(exportAuditRepository, atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getStatus()).isEqualTo("FAILED");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setPseudo("Alice");
        user.setPasswordHash("hash");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setGeographicZone("METROPOLE");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setTimezoneName("Europe/Paris");
        user.setCreatedAt(Instant.parse("2026-03-20T09:00:00Z"));
        user.setActivatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        return user;
    }

    private TaskDefinitionEntity taskDefinition() {
        TaskDefinitionEntity entity = new TaskDefinitionEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setTitle("Lecture");
        entity.setIcon("📚");
        entity.setDescription("25 minutes");
        entity.setTaskType("RECURRING");
        entity.setCreatedAt(Instant.parse("2026-03-20T09:10:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-03-21T09:10:00Z"));
        return entity;
    }

    private TaskOccurrenceEntity occurrence(UUID userId, String status) {
        TaskOccurrenceEntity entity = new TaskOccurrenceEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTaskDefinitionId(UUID.randomUUID());
        entity.setTaskRuleId(UUID.randomUUID());
        entity.setOccurrenceDate(LocalDate.of(2026, 3, 26));
        entity.setOccurrenceTime(LocalTime.of(8, 0));
        entity.setStatus(status);
        entity.setDayCategory("WORKDAY");
        entity.setCreatedAt(Instant.parse("2026-03-26T07:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-03-26T08:10:00Z"));
        return entity;
    }
}
