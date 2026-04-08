package com.ia.api.stats.api;

import com.ia.api.common.api.GlobalExceptionHandler;
import com.ia.api.stats.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatsControllerTest {

    private StatsService statsService;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        statsService = Mockito.mock(StatsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StatsController(statsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new TestingAuthenticationToken("alice@example.com", null);
    }

    @Test
    void returnsDashboard() throws Exception {
        StatsPeriodResponse period = new StatsPeriodResponse(
                "MONTHLY",
                "Mensuel",
                new StatsSnapshotResponse("2026-03-01", "2026-03-31", 12, 9, 1, 1, 1, 4, 75),
                new StatsSnapshotResponse("2026-02-01", "2026-02-28", 8, 5, 2, 1, 0, 3, 62),
                new StatsDeltaResponse(4, 4, 13),
                "Ce mois vs le mois precedent",
                List.of(new StatsTaskSummaryResponse(UUID.randomUUID(), "Lecture", "📚", 4, 3, 1, 0, 0, 75, 1, 15)),
                List.of()
        );

        Mockito.when(statsService.getDashboard("alice@example.com", null, null, null, null))
                .thenReturn(new StatsDashboardResponse("2026-03-27T10:00:00Z", "2026-03-20T08:00:00Z", period, period, period, period));

        mockMvc.perform(get("/api/v1/stats/dashboard").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthly.label").value("Mensuel"))
                .andExpect(jsonPath("$.data.monthly.taskBreakdown[0].title").value("Lecture"))
                .andExpect(jsonPath("$.data.daily.label").value("Mensuel"));
    }

    @Test
    void returnsTaskDetail() throws Exception {
        UUID taskId = UUID.randomUUID();
        Mockito.when(statsService.getTaskDetail("alice@example.com", taskId, "MONTHLY", null))
                .thenReturn(new StatsTaskDetailResponse(
                        taskId,
                        "Lecture",
                        "📚",
                        "MONTHLY",
                        "Mensuel",
                        new StatsSnapshotResponse("2026-03-01", "2026-03-31", 5, 4, 1, 0, 0, 1, 80),
                        new StatsSnapshotResponse("2026-02-01", "2026-02-28", 4, 2, 1, 1, 0, 1, 50),
                        new StatsDeltaResponse(1, 2, 30),
                        List.of(new StatsTaskRecentOccurrenceResponse("2026-03-26", "08:00", "done", "WORKDAY"))
                ));

        mockMvc.perform(get("/api/v1/stats/tasks/{taskDefinitionId}", taskId)
                        .param("periodType", "MONTHLY")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Lecture"))
                .andExpect(jsonPath("$.data.recentOccurrences[0].status").value("done"));
    }
}
