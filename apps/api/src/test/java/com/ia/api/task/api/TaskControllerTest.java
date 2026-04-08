package com.ia.api.task.api;

import com.ia.api.common.api.GlobalExceptionHandler;
import com.ia.api.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerTest {
    private TaskService taskService;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        taskService = Mockito.mock(TaskService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(taskService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new TestingAuthenticationToken("alice@example.com", null);
    }

    @Test
    void createTaskAcceptsMultipartAndReturnsTask() throws Exception {
        TaskResponse response = taskResponse();
        Mockito.when(taskService.createTask(eq("alice@example.com"), any(CreateTaskRequest.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/tasks")
                        .param("title", "Meditation")
                        .param("icon", "🧘")
                        .param("startDate", LocalDate.now().plusDays(1).toString())
                        .param("dayCategories[0]", "WORKDAY")
                        .param("timeMode", "FIXED")
                        .param("fixedTime", "07:00")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Meditation"))
                .andExpect(jsonPath("$.data.taskType").value("ONE_TIME"));
    }

    @Test
    void createTaskReturnsBadRequestForPastDate() throws Exception {
        Mockito.when(taskService.createTask(any(), any()))
                .thenThrow(new IllegalArgumentException("La date de debut ne peut pas etre dans le passe."));

        mockMvc.perform(multipart("/api/v1/tasks")
                        .param("title", "Meditation")
                        .param("icon", "🧘")
                        .param("startDate", LocalDate.now().minusDays(1).toString())
                        .param("dayCategories[0]", "WORKDAY")
                        .param("timeMode", "FIXED")
                        .param("fixedTime", "07:00")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .principal(authentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void previewNextOccurrenceReturnsOccurrenceLabel() throws Exception {
        TaskPreviewResponse preview = new TaskPreviewResponse(
                LocalDate.now().plusDays(1),
                LocalTime.of(7, 0),
                "Le 25/03/2026 a 07:00"
        );
        Mockito.when(taskService.previewNextOccurrence(eq("alice@example.com"), any(TaskPreviewRequest.class)))
                .thenReturn(preview);

        mockMvc.perform(post("/api/v1/tasks/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "%s",
                                  "dayCategories": ["WORKDAY"],
                                  "timeMode": "FIXED",
                                  "fixedTime": "07:00"
                                }
                                """.formatted(LocalDate.now().plusDays(1)))
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occurrenceLabel").value("Le 25/03/2026 a 07:00"));
    }

    @Test
    void listOccurrencesReturnsPagedTaskRows() throws Exception {
        Mockito.when(taskService.listOccurrences(eq("alice@example.com"), any(TaskOccurrenceListRequest.class))).thenReturn(
                new TaskOccurrencePageResponse(
                        List.of(
                                new TaskOccurrenceResponse(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        "Sport",
                                        "🏃",
                                        null,
                                        "RECURRING",
                                        "WAKE_UP_OFFSET",
                                        null,
                                        30,
                                        List.of("WORKDAY"),
                                        "WEEKLY",
                                        List.of(1, 3, 5),
                                        null,
                                        LocalDate.now().plusWeeks(8),
                                        null,
                                        LocalDate.now().plusDays(1),
                                        LocalTime.of(8, 0),
                                        "planned",
                                        "WORKDAY",
                                        false,
                                        true,
                                        true,
                                        null,
                                        1,
                                        List.of()
                                )
                        ),
                        0,
                        10,
                        1,
                        1
                )
        );

        mockMvc.perform(get("/api/v1/tasks/occurrences")
                        .param("page", "0")
                        .param("size", "10")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("Sport"))
                .andExpect(jsonPath("$.data.items[0].timeMode").value("WAKE_UP_OFFSET"))
                .andExpect(jsonPath("$.data.items[0].futureScopeAvailable").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void updateOccurrenceAcceptsScopePayload() throws Exception {
        UUID occurrenceId = UUID.randomUUID();
        Mockito.when(taskService.updateOccurrence(eq("alice@example.com"), eq(occurrenceId), any(UpdateTaskOccurrenceRequest.class)))
                .thenReturn(new TaskOccurrenceResponse(
                        occurrenceId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Lecture",
                        "📚",
                        null,
                        "RECURRING",
                        "FIXED",
                        LocalTime.of(21, 0),
                        null,
                        List.of("WORKDAY"),
                        "WEEKLY",
                        List.of(1, 3, 5),
                        null,
                        LocalDate.now().plusWeeks(6),
                        null,
                        LocalDate.now().plusDays(2),
                        LocalTime.of(21, 0),
                        "planned",
                        "WORKDAY",
                        false,
                        true,
                        true,
                        null,
                        1,
                        List.of()
                ));

        mockMvc.perform(put("/api/v1/tasks/occurrences/{occurrenceId}", occurrenceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "THIS_OCCURRENCE",
                                  "title": "Lecture",
                                  "icon": "📚",
                                  "description": "Texte",
                                  "timeMode": "FIXED",
                                  "fixedTime": "21:00"
                                }
                                """)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.occurrenceTime").value("21:00:00"));
    }

    @Test
    void deleteOccurrenceAcceptsScopePayload() throws Exception {
        UUID occurrenceId = UUID.randomUUID();
        Mockito.when(taskService.deleteOccurrence(eq("alice@example.com"), eq(occurrenceId), any(DeleteTaskOccurrenceRequest.class)))
                .thenReturn(new com.ia.api.auth.api.MessageResponse("Occurrence unique supprimee."));

        mockMvc.perform(delete("/api/v1/tasks/occurrences/{occurrenceId}", occurrenceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "THIS_OCCURRENCE"
                                }
                                """)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Occurrence unique supprimee."));
    }

    private TaskResponse taskResponse() {
        return new TaskResponse(
                UUID.randomUUID(),
                "Meditation",
                "🧘",
                null,
                "ONE_TIME",
                LocalDate.now().plusDays(1),
                List.of("WORKDAY"),
                "FIXED",
                LocalTime.of(7, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
