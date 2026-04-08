package com.ia.api.auth.service;

import com.ia.api.auth.api.ActivationResponse;
import com.ia.api.auth.api.LoginRequest;
import com.ia.api.auth.api.LoginResponse;
import com.ia.api.auth.api.MessageResponse;
import com.ia.api.auth.api.PasswordResetConfirmRequest;
import com.ia.api.auth.api.PasswordResetRequest;
import com.ia.api.auth.api.RefreshResponse;
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
import com.ia.api.user.domain.DayCategory;
import com.ia.api.user.domain.DayProfileEntity;
import com.ia.api.user.repository.AssetRepository;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.user.service.ProfilePhotoStorageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final ConsentRepository consentRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ResetPasswordTokenRepository resetPasswordTokenRepository;
    private final AssetRepository assetRepository;
    private final DayProfileRepository dayProfileRepository;
    private final HolidaySyncStateService holidaySyncStateService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ActivationEmailSender activationEmailSender;
    private final PasswordResetEmailSender passwordResetEmailSender;
    private final ProfilePhotoStorageService profilePhotoStorageService;
    private final String frontendUrl;
    private final long refreshTokenDays;

    public AuthService(
            UserRepository userRepository,
            ConsentRepository consentRepository,
            ActivationTokenRepository activationTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            ResetPasswordTokenRepository resetPasswordTokenRepository,
            AssetRepository assetRepository,
            DayProfileRepository dayProfileRepository,
            HolidaySyncStateService holidaySyncStateService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            ActivationEmailSender activationEmailSender,
            PasswordResetEmailSender passwordResetEmailSender,
            ProfilePhotoStorageService profilePhotoStorageService,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.jwt.refresh-token-days}") long refreshTokenDays
    ) {
        this.userRepository = userRepository;
        this.consentRepository = consentRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetPasswordTokenRepository = resetPasswordTokenRepository;
        this.assetRepository = assetRepository;
        this.dayProfileRepository = dayProfileRepository;
        this.holidaySyncStateService = holidaySyncStateService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.activationEmailSender = activationEmailSender;
        this.passwordResetEmailSender = passwordResetEmailSender;
        this.profilePhotoStorageService = profilePhotoStorageService;
        this.frontendUrl = frontendUrl;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request, String clientIp) {
        if (!Boolean.TRUE.equals(request.getConsentAccepted())) {
            throw new IllegalArgumentException("Le consentement doit être accepté");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Un compte existe déjà pour cet email");
        }

        Instant now = Instant.now();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setPseudo(request.getPseudo());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setBirthDate(request.getBirthDate());
        user.setGeographicZone(request.getGeographicZone());
        user.setAccountStatus(AccountStatus.PENDING_ACTIVATION);
        user.setTimezoneName("Europe/Paris");
        user.setCreatedAt(now);
        userRepository.save(user);
        createDefaultDayProfiles(user.getId());
        holidaySyncStateService.markPending(user.getId(), user.getGeographicZone());

        attachProfilePhotoIfPresent(user, request.getProfilePhoto(), now);
        userRepository.save(user);

        ConsentEntity consent = new ConsentEntity();
        consent.setId(UUID.randomUUID());
        consent.setUserId(user.getId());
        consent.setConsentType("DATA_COLLECTION");
        consent.setAcceptedAt(now);
        consent.setLegalVersion(request.getLegalVersion());
        consent.setIpHash(hashIp(clientIp));
        consentRepository.save(consent);

        // P-12: invalidate any previous unused activation tokens for this user
        activationTokenRepository.findActiveByUserId(user.getId(), now)
                .forEach(existing -> existing.setUsedAt(now));

        String rawToken = UUID.randomUUID().toString();
        ActivationTokenEntity token = new ActivationTokenEntity();
        token.setId(UUID.randomUUID());
        token.setUserId(user.getId());
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(now.plus(1, ChronoUnit.HOURS));
        activationTokenRepository.save(token);

        String activationLink = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/activate")
                .queryParam("token", rawToken)
                .toUriString();
        activationEmailSender.sendActivationEmail(user.getEmail(), user.getPseudo(), activationLink);

        return new RegisterResponse(user.getId(), user.getAccountStatus().name(), user.getEmail());
    }

    @Transactional
    public ActivationResponse activate(String tokenValue) {
        // P-15: pessimistic lock prevents concurrent use of the same token
        ActivationTokenEntity token = activationTokenRepository.findByTokenHashForUpdate(hashToken(tokenValue))
                .orElseThrow(() -> new IllegalArgumentException("Jeton d'activation introuvable"));
        if (token.getUsedAt() != null) {
            throw new IllegalArgumentException("Jeton d'activation déjà utilisé");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Jeton d'activation expiré");
        }

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setActivatedAt(Instant.now());
        token.setUsedAt(Instant.now());
        return new ActivationResponse(user.getId(), user.getAccountStatus().name());
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Le compte n'est pas activé");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides");
        }

        return createSession(user);
    }

    @Transactional
    public AuthSession refresh(String rawRefreshToken) {
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(() -> new BadCredentialsException("Jeton de rafraîchissement invalide"));
        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Jeton de rafraîchissement invalide");
        }

        UserEntity user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Jeton de rafraîchissement invalide"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Le compte n'est pas activé");
        }

        refreshToken.setRevokedAt(Instant.now());
        return createSession(user);
    }

    @Transactional
    public MessageResponse requestPasswordReset(PasswordResetRequest request) {
        userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .filter(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
                .ifPresent(this::createAndSendPasswordResetToken);

        return new MessageResponse("Si le compte existe, un email de réinitialisation a été envoyé.");
    }

    @Transactional
    public MessageResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        ResetPasswordTokenEntity resetToken = resetPasswordTokenRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Jeton de réinitialisation introuvable"));
        if (resetToken.getUsedAt() != null) {
            throw new IllegalArgumentException("Jeton de réinitialisation déjà utilisé");
        }
        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Jeton de réinitialisation expiré");
        }

        UserEntity user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsedAt(Instant.now());
        revokeActiveRefreshTokens(user.getId(), Instant.now());

        return new MessageResponse("Mot de passe réinitialisé avec succès.");
    }

    @Transactional
    public MessageResponse logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                    .filter(token -> token.getRevokedAt() == null)
                    .ifPresent(token -> token.setRevokedAt(Instant.now()));
        }
        return new MessageResponse("Déconnexion effectuée.");
    }

    private AuthSession createSession(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(hashToken(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        return new AuthSession(
                new LoginResponse(user.getId(), accessToken, jwtService.getAccessTokenExpiresInSeconds()),
                rawRefreshToken
        );
    }

    private void createAndSendPasswordResetToken(UserEntity user) {
        // P-17: invalidate any existing active reset tokens before creating a new one
        resetPasswordTokenRepository.findAllByUserIdAndUsedAtIsNull(user.getId())
                .forEach(existing -> existing.setUsedAt(Instant.now()));

        String rawToken = UUID.randomUUID().toString();

        ResetPasswordTokenEntity token = new ResetPasswordTokenEntity();
        token.setId(UUID.randomUUID());
        token.setUserId(user.getId());
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        resetPasswordTokenRepository.save(token);

        String resetLink = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .toUriString();
        passwordResetEmailSender.sendPasswordResetEmail(user.getEmail(), user.getPseudo(), resetLink);
    }

    private void revokeActiveRefreshTokens(UUID userId, Instant revokedAt) {
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(token -> token.setRevokedAt(revokedAt));
    }

    private void attachProfilePhotoIfPresent(UserEntity user, MultipartFile profilePhoto, Instant now) {
        if (profilePhoto == null || profilePhoto.isEmpty()) {
            return;
        }

        try {
            ProfilePhotoStorageService.StoredPhoto storedPhoto = profilePhotoStorageService.store(user.getId(), profilePhoto);

            AssetEntity asset = new AssetEntity();
            asset.setId(UUID.randomUUID());
            asset.setUserId(user.getId());
            asset.setAssetType("PROFILE_PHOTO");
            asset.setStorageKey(storedPhoto.storageKey());
            asset.setCreatedAt(now);
            assetRepository.save(asset);

            user.setProfilePhotoUrl(storedPhoto.storageKey());
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de sauvegarder la photo de profil", exception);
        }
    }

    private void createDefaultDayProfiles(UUID userId) {
        dayProfileRepository.save(createDayProfile(userId, DayCategory.WORKDAY, "07:00"));
        dayProfileRepository.save(createDayProfile(userId, DayCategory.VACATION, "09:00"));
        dayProfileRepository.save(createDayProfile(userId, DayCategory.WEEKEND_HOLIDAY, "08:30"));
    }

    private DayProfileEntity createDayProfile(UUID userId, DayCategory category, String wakeUpTime) {
        DayProfileEntity profile = new DayProfileEntity();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setDayCategory(category);
        profile.setWakeUpTime(java.time.LocalTime.parse(wakeUpTime));
        return profile;
    }

    private String hashIp(String clientIp) {
        return digestValue(clientIp);
    }

    private String hashToken(String rawToken) {
        return digestValue(rawToken);
    }

    private String digestValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Impossible de calculer le hash", exception);
        }
    }

    public record AuthSession(LoginResponse response, String refreshToken) {
    }
}
