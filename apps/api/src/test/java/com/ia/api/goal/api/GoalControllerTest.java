package com.ia.api.goal.api;

import com.ia.api.auth.api.MessageResponse;
import com.ia.api.common.api.GlobalExceptionHandler;
import com.ia.api.goal.service.GoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalControllerTest {

    private GoalService goalService;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        goalService = Mockito.mock(GoalService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GoalController(goalService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new TestingAuthenticationToken("alice@example.com", null);
    }

    @Test
    void listsGoalsAndEligibleTasks() throws Exception {
        UUID goalId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Mockito.when(goalService.listGoals("alice@example.com"))
                .thenReturn(new GoalListResponse(
                        List.of(new GoalResponse(
                                goalId,
                                "TASK",
                                "WEEKLY",
                                4,
                                true,
                                taskId,
                                "Lecture",
                                "📚",
                                "WEEKLY",
                                new GoalProgressSnapshot("2026-03-23", "2026-03-29", 2, 4, 2, 50, false, "IN_PROGRESS"),
                                List.of(),
                                "2026-03-26T10:00:00Z",
                                "2026-03-26T10:00:00Z"
                        )),
                        List.of(),
                        List.of(new GoalEligibleTaskItem(taskId, "Lecture", "📚", "WEEKLY")),
                        "2026-03-24T10:00:00Z"
                ));

        mockMvc.perform(get("/api/v1/goals").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goals[0].taskTitle").value("Lecture"))
                .andExpect(jsonPath("$.data.inactiveGoals").isArray())
                .andExpect(jsonPath("$.data.accountCreatedAt").value("2026-03-24T10:00:00Z"))
                .andExpect(jsonPath("$.data.eligibleTasks[0].recurrenceType").value("WEEKLY"));
    }

    @Test
    void createsGoal() throws Exception {
        UUID goalId = UUID.randomUUID();
        Mockito.when(goalService.createGoal(eq("alice@example.com"), eq(new CreateGoalRequest("GLOBAL", "MONTHLY", 10, null))))
                .thenReturn(new GoalResponse(
                        goalId,
                        "GLOBAL",
                        "MONTHLY",
                        10,
                        true,
                        null,
                        null,
                        null,
                        null,
                        new GoalProgressSnapshot("2026-03-01", "2026-03-31", 3, 10, 7, 30, false, "IN_PROGRESS"),
                        List.of(),
                        "2026-03-26T10:00:00Z",
                        "2026-03-26T10:00:00Z"
                ));

        mockMvc.perform(post("/api/v1/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalScope": "GLOBAL",
                                  "periodType": "MONTHLY",
                                  "targetCount": 10
                                }
                                """)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodType").value("MONTHLY"))
                .andExpect(jsonPath("$.data.targetCount").value(10));
    }

    @Test
    void deletesGoal() throws Exception {
        UUID goalId = UUID.randomUUID();
        Mockito.when(goalService.deleteGoal("alice@example.com", goalId))
                .thenReturn(new MessageResponse("Objectif supprimé."));

        mockMvc.perform(delete("/api/v1/goals/{goalId}", goalId).principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Objectif supprimé."));
    }
}
