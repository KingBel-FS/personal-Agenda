package com.ia.api.auth.api;

import com.ia.api.auth.service.AuthService;
import com.ia.api.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {
    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = Mockito.mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, false))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerAcceptsMultipartPayload() throws Exception {
        Mockito.when(authService.register(any(RegisterRequest.class), eq("127.0.0.1")))
                .thenReturn(new RegisterResponse(UUID.randomUUID(), "PENDING_ACTIVATION", "alice@example.com"));

        mockMvc.perform(multipart("/api/v1/auth/register")
                        .file(new MockMultipartFile("profilePhoto", "avatar.png", "image/png", "file".getBytes()))
                        .param("pseudo", "alice")
                        .param("email", "alice@example.com")
                        .param("password", "Password123")
                        .param("birthDate", "1995-05-12")
                        .param("geographicZone", "METROPOLE")
                        .param("legalVersion", "v1")
                        .param("consentAccepted", "true")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING_ACTIVATION"));
    }

    @Test
    void loginReturnsAccessTokenAndRefreshCookie() throws Exception {
        Mockito.when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthService.AuthSession(
                        new LoginResponse(UUID.randomUUID(), "jwt-token", 900),
                        "refresh-token"
                ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"Password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("pht_refresh_token=refresh-token")));
    }

    @Test
    void loginReturnsStandardUnauthorizedOnBadCredentials() throws Exception {
        Mockito.when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"bad"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void refreshRotatesCookieAndReturnsAccessToken() throws Exception {
        Mockito.when(authService.refresh("refresh-token"))
                .thenReturn(new AuthService.AuthSession(
                        new LoginResponse(UUID.randomUUID(), "new-jwt", 900),
                        "new-refresh"
                ));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new jakarta.servlet.http.Cookie("pht_refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-jwt"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("pht_refresh_token=new-refresh")));
    }

    @Test
    void logoutClearsRefreshCookie() throws Exception {
        Mockito.when(authService.logout("refresh-token"))
                .thenReturn(new MessageResponse("Déconnexion effectuée."));

        mockMvc.perform(post("/api/v1/auth/logout").cookie(new jakarta.servlet.http.Cookie("pht_refresh_token", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Déconnexion effectuée."))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    void passwordResetRequestReturnsGenericMessage() throws Exception {
        Mockito.when(authService.requestPasswordReset(any(PasswordResetRequest.class)))
                .thenReturn(new MessageResponse("Si le compte existe, un email de réinitialisation a été envoyé."));

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Si le compte existe, un email de réinitialisation a été envoyé."));
    }

    @Test
    void passwordResetConfirmReturnsSuccessMessage() throws Exception {
        Mockito.when(authService.confirmPasswordReset(any(PasswordResetConfirmRequest.class)))
                .thenReturn(new MessageResponse("Mot de passe réinitialisé avec succès."));

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"abc","newPassword":"NewPassword123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Mot de passe réinitialisé avec succès."));
    }

    @Test
    void activateDelegatesToService() throws Exception {
        Mockito.when(authService.activate("token"))
                .thenReturn(new ActivationResponse(UUID.randomUUID(), "ACTIVE"));

        mockMvc.perform(get("/api/v1/auth/activate").param("token", "token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
