package com.ia.api.agenda;

import com.ia.api.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgendaControllerTest {

    private AgendaService agendaService;
    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        agendaService = Mockito.mock(AgendaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AgendaController(agendaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new TestingAuthenticationToken("alice@example.com", null);
    }

    @Test
    void returnsWeekAgenda() throws Exception {
        Mockito.when(agendaService.getWeek(eq("alice@example.com"), eq(LocalDate.of(2026, 4, 15))))
                .thenReturn(new AgendaRangeResponse(
                        "week",
                        LocalDate.of(2026, 4, 15),
                        LocalDate.of(2026, 4, 13),
                        LocalDate.of(2026, 4, 19),
                        List.of(new AgendaDaySummary(
                                LocalDate.of(2026, 4, 13),
                                true,
                                false,
                                false,
                                false,
                                "WORKDAY",
                                2,
                                1,
                                1,
                                0,
                                0,
                                "mixed",
                                List.of("📚", "🏃")
                        ))
                ));

        mockMvc.perform(get("/api/v1/agenda/week")
                        .param("date", "2026-04-15")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view").value("week"))
                .andExpect(jsonPath("$.data.rangeStart").value("2026-04-13"))
                .andExpect(jsonPath("$.data.days[0].icons[0]").value("📚"));
    }

    @Test
    void returnsMonthAgenda() throws Exception {
        Mockito.when(agendaService.getMonth(eq("alice@example.com"), eq(LocalDate.of(2026, 4, 15))))
                .thenReturn(new AgendaRangeResponse(
                        "month",
                        LocalDate.of(2026, 4, 15),
                        LocalDate.of(2026, 3, 30),
                        LocalDate.of(2026, 5, 3),
                        List.of(new AgendaDaySummary(
                                LocalDate.of(2026, 3, 30),
                                false,
                                true,
                                false,
                                false,
                                "WORKDAY",
                                0,
                                0,
                                0,
                                0,
                                0,
                                "empty",
                                List.of()
                        ))
                ));

        mockMvc.perform(get("/api/v1/agenda/month")
                        .param("date", "2026-04-15")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.view").value("month"))
                .andExpect(jsonPath("$.data.days[0].currentMonth").value(false))
                .andExpect(jsonPath("$.data.rangeEnd").value("2026-05-03"));
    }
}
