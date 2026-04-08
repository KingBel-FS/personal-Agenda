package com.ia.api.sync.api;

import com.ia.api.auth.service.AppUserDetailsService;
import com.ia.api.auth.service.JwtService;
import com.ia.api.sync.service.RealtimeSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SyncControllerTest {

    private JwtService jwtService;
    private AppUserDetailsService userDetailsService;
    private RealtimeSyncService realtimeSyncService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jwtService = Mockito.mock(JwtService.class);
        userDetailsService = Mockito.mock(AppUserDetailsService.class);
        realtimeSyncService = Mockito.mock(RealtimeSyncService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SyncController(jwtService, userDetailsService, realtimeSyncService))
                .build();
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        Mockito.when(jwtService.extractUsername("bad-token")).thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(get("/api/v1/sync/events").param("access_token", "bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void opensSseStreamForValidToken() throws Exception {
        UserDetails userDetails = User.withUsername("alice@example.com").password("n/a").authorities("USER").build();
        Mockito.when(jwtService.extractUsername("good-token")).thenReturn("alice@example.com");
        Mockito.when(userDetailsService.loadUserByUsername("alice@example.com")).thenReturn(userDetails);
        Mockito.when(jwtService.isTokenValid("good-token", "alice@example.com")).thenReturn(true);
        Mockito.when(realtimeSyncService.subscribe(anyString())).thenReturn(new SseEmitter(0L));

        mockMvc.perform(get("/api/v1/sync/events")
                        .param("access_token", "good-token")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }
}
