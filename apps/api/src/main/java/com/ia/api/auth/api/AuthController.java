package com.ia.api.auth.api;

import com.ia.api.auth.service.AuthService;
import com.ia.api.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "pht_refresh_token";
    private final AuthService authService;
    private final boolean secureCookies;

    public AuthController(
            AuthService authService,
            @Value("${app.cookie.secure:false}") boolean secureCookies
    ) {
        this.authService = authService;
        this.secureCookies = secureCookies;
    }

    @PostMapping(path = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RegisterResponse> register(@Valid @ModelAttribute RegisterRequest request,
                                                  HttpServletRequest httpServletRequest) {
        return ApiResponse.of(authService.register(request, httpServletRequest.getRemoteAddr()));
    }

    @GetMapping("/activate")
    public ApiResponse<ActivationResponse> activate(@RequestParam String token) {
        return ApiResponse.of(authService.activate(token));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.AuthSession session = authService.login(request);
        writeRefreshCookie(response, session.refreshToken());
        return ApiResponse.of(session.response());
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, defaultValue = "") String refreshToken,
            HttpServletResponse response
    ) {
        AuthService.AuthSession session = authService.refresh(refreshToken);
        writeRefreshCookie(response, session.refreshToken());
        return ApiResponse.of(new RefreshResponse(
                session.response().accessToken(),
                session.response().expiresInSeconds()
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<MessageResponse> logout(
            @CookieValue(name = REFRESH_COOKIE, defaultValue = "") String refreshToken,
            HttpServletResponse response
    ) {
        clearRefreshCookie(response);
        return ApiResponse.of(authService.logout(refreshToken));
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<MessageResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return ApiResponse.of(authService.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<MessageResponse> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return ApiResponse.of(authService.confirmPasswordReset(request));
    }

    private void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(secureCookies)
                .path("/api/v1/auth")
                .sameSite("Strict")
                .maxAge(30L * 24L * 60L * 60L)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookies)
                .path("/api/v1/auth")
                .sameSite("Strict")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
