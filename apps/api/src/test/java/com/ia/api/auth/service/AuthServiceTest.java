package com.ia.api.auth.service;

import com.ia.api.auth.api.ActivationResponse;
import com.ia.api.auth.api.LoginRequest;
import com.ia.api.auth.api.MessageResponse;
import com.ia.api.auth.api.PasswordResetConfirmRequest;
import com.ia.api.auth.api.PasswordResetRequest;
import com.ia.api.auth.api.RegisterRequest;
import com.ia.api.auth.api.RegisterResponse;
import com.ia.api.auth.domain.AccountStatus;
import com.ia.api.auth.domain.ActivationTokenEntity;
import com.ia.api.auth.domain.ConsentEntity;
import com.ia.api.auth.domain.RefreshTokenEntity;
import com.ia.api.auth.domain.ResetPasswordTokenEntity;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.ActivationTokenRepository;
import com.ia.api.auth.repository.ConsentRepository;
import com.ia.api.auth.repository.RefreshTokenRepository;
import com.ia.api.auth.repository.ResetPasswordTokenRepository;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.holiday.service.HolidaySyncStateService;
import com.ia.api.user.domain.AssetEntity;
import com.ia.api.user.repository.AssetRepository;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.user.service.ProfilePhotoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private ConsentRepository consentRepository;
    @Mock
    private ActivationTokenRepository activationTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private ResetPasswordTokenRepository resetPasswordTokenRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private DayProfileRepository dayProfileRepository;
    @Mock
    private HolidaySyncStateService holidaySyncStateService;
    @Mock
    private JwtService jwtService;
    @Mock
    private ActivationEmailSender activationEmailSender;
    @Mock
    private PasswordResetEmailSender passwordResetEmailSender;
    @Mock
    private ProfilePhotoStorageService profilePhotoStorageService;

    @Captor
    private ArgumentCaptor<UserEntity> userCaptor;
    @Captor
    private ArgumentCaptor<ConsentEntity> consentCaptor;
    @Captor
    private ArgumentCaptor<ActivationTokenEntity> tokenCaptor;
    @Captor
    private ArgumentCaptor<AssetEntity> assetCaptor;
    @Captor
    private ArgumentCaptor<RefreshTokenEntity> refreshTokenCaptor;
    @Captor
    private ArgumentCaptor<ResetPasswordTokenEntity> resetTokenCaptor;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        authService = new AuthService(
                userRepository,
                consentRepository,
                activationTokenRepository,
                refreshTokenRepository,
                resetPasswordTokenRepository,
                assetRepository,
                dayProfileRepository,
                holidaySyncStateService,
                jwtService,
                passwordEncoder,
                activationEmailSender,
                passwordResetEmailSender,
                profilePhotoStorageService,
                "http://localhost:4200",
                30
        );
    }

    @Test
    void registerStoresPendingUserConsentAssetAndToken() throws IOException {
        RegisterRequest request = new RegisterRequest();
        request.setPseudo("alice");
        request.setEmail("Alice@Example.com");
        request.setPassword("Password123");
        request.setBirthDate(LocalDate.of(1995, 5, 12));
        request.setGeographicZone("METROPOLE");
        request.setConsentAccepted(true);
        request.setLegalVersion("v1");
        request.setProfilePhoto(new MockMultipartFile("profilePhoto", "avatar.png", "image/png", "image".getBytes()));

        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
        when(profilePhotoStorageService.store(any(UUID.class), any()))
                .thenReturn(new ProfilePhotoStorageService.StoredPhoto("user/photo.png"));

        RegisterResponse response = authService.register(request, "127.0.0.1");

        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        verify(consentRepository).save(consentCaptor.capture());
        verify(activationTokenRepository).save(tokenCaptor.capture());
        verify(assetRepository).save(assetCaptor.capture());

        UserEntity savedUser = userCaptor.getAllValues().getLast();
        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(savedUser.getProfilePhotoUrl()).isEqualTo("user/photo.png");
        assertThat(savedUser.getTimezoneName()).isEqualTo("Europe/Paris");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Password123");

        ConsentEntity savedConsent = consentCaptor.getValue();
        assertThat(savedConsent.getUserId()).isEqualTo(savedUser.getId());
        assertThat(savedConsent.getLegalVersion()).isEqualTo("v1");
        assertThat(savedConsent.getIpHash()).isNotBlank();

        ActivationTokenEntity savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUserId()).isEqualTo(savedUser.getId());
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now().plusSeconds(3000));

        AssetEntity savedAsset = assetCaptor.getValue();
        assertThat(savedAsset.getUserId()).isEqualTo(savedUser.getId());
        assertThat(savedAsset.getAssetType()).isEqualTo("PROFILE_PHOTO");

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(activationEmailSender).sendActivationEmail(
                eq(savedUser.getEmail()),
                eq(savedUser.getPseudo()),
                linkCaptor.capture()
        );
        assertThat(linkCaptor.getValue()).startsWith("http://localhost:4200/activate?token=");

        assertThat(response.status()).isEqualTo("PENDING_ACTIVATION");
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void loginReturnsAccessTokenAndPersistsHashedRefreshToken() {
        UserEntity user = activeUser();
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn("jwt-token");
        when(jwtService.getAccessTokenExpiresInSeconds()).thenReturn(900L);

        AuthService.AuthSession session = authService.login(new LoginRequest("alice@example.com", "Password123"));

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshTokenEntity refreshToken = refreshTokenCaptor.getValue();
        assertThat(refreshToken.getUserId()).isEqualTo(user.getId());
        assertThat(refreshToken.getTokenHash()).isNotBlank().isNotEqualTo(session.refreshToken());
        assertThat(session.response().accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void loginRejectsInvalidCredentials() {
        UserEntity user = activeUser();
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "bad")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshRotatesRefreshToken() {
        UserEntity user = activeUser();
        RefreshTokenEntity existing = new RefreshTokenEntity();
        existing.setId(UUID.randomUUID());
        existing.setUserId(user.getId());
        existing.setTokenHash(digest("refresh-raw"));
        existing.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenHash(digest("refresh-raw"))).thenReturn(Optional.of(existing));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn("jwt-rotated");
        when(jwtService.getAccessTokenExpiresInSeconds()).thenReturn(900L);

        AuthService.AuthSession session = authService.refresh("refresh-raw");

        assertThat(existing.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(session.response().accessToken()).isEqualTo("jwt-rotated");
    }

    @Test
    void requestPasswordResetStoresTokenAndSendsEmail() {
        UserEntity user = activeUser();
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        MessageResponse response = authService.requestPasswordReset(new PasswordResetRequest("alice@example.com"));

        verify(resetPasswordTokenRepository).save(resetTokenCaptor.capture());
        ResetPasswordTokenEntity saved = resetTokenCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plusSeconds(3000));
        verify(passwordResetEmailSender).sendPasswordResetEmail(eq(user.getEmail()), eq(user.getPseudo()), any(String.class));
        assertThat(response.message()).contains("Si le compte existe");
    }

    @Test
    void confirmPasswordResetChangesPasswordAndRevokesRefreshTokens() {
        UserEntity user = activeUser();
        ResetPasswordTokenEntity resetToken = new ResetPasswordTokenEntity();
        resetToken.setId(UUID.randomUUID());
        resetToken.setUserId(user.getId());
        resetToken.setTokenHash(digest("reset-token"));
        resetToken.setExpiresAt(Instant.now().plusSeconds(3600));

        RefreshTokenEntity activeRefresh = new RefreshTokenEntity();
        activeRefresh.setId(UUID.randomUUID());
        activeRefresh.setUserId(user.getId());
        activeRefresh.setExpiresAt(Instant.now().plusSeconds(3600));

        when(resetPasswordTokenRepository.findByTokenHash(digest("reset-token"))).thenReturn(Optional.of(resetToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId())).thenReturn(List.of(activeRefresh));

        MessageResponse response = authService.confirmPasswordReset(
                new PasswordResetConfirmRequest("reset-token", "NewPassword123")
        );

        assertThat(resetToken.getUsedAt()).isNotNull();
        assertThat(activeRefresh.getRevokedAt()).isNotNull();
        assertThat(response.message()).isEqualTo("Mot de passe réinitialisé avec succès.");
        assertThat(user.getPasswordHash()).isNotEqualTo("hash");
    }

    @Test
    void logoutRevokesCurrentRefreshTokenWhenPresent() {
        RefreshTokenEntity activeRefresh = new RefreshTokenEntity();
        activeRefresh.setId(UUID.randomUUID());
        activeRefresh.setUserId(UUID.randomUUID());
        activeRefresh.setTokenHash(digest("refresh-raw"));

        when(refreshTokenRepository.findByTokenHash(digest("refresh-raw"))).thenReturn(Optional.of(activeRefresh));

        MessageResponse response = authService.logout("refresh-raw");

        assertThat(activeRefresh.getRevokedAt()).isNotNull();
        assertThat(response.message()).isEqualTo("Déconnexion effectuée.");
    }

    @Test
    void activateMarksUserActiveAndTokenUsed() {
        UUID userId = UUID.randomUUID();
        ActivationTokenEntity token = new ActivationTokenEntity();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setTokenHash(digest("token"));
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash");
        user.setAccountStatus(AccountStatus.PENDING_ACTIVATION);

        when(activationTokenRepository.findByTokenHashForUpdate(digest("token"))).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ActivationResponse response = authService.activate("token");

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getActivatedAt()).isNotNull();
        assertThat(token.getUsedAt()).isNotNull();
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setPseudo("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setTimezoneName("Europe/Paris");
        return user;
    }

    private String digest(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
