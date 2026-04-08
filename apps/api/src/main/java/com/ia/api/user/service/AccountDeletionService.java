package com.ia.api.user.service;

import com.ia.api.auth.repository.ActivationTokenRepository;
import com.ia.api.auth.repository.ConsentRepository;
import com.ia.api.auth.repository.RefreshTokenRepository;
import com.ia.api.auth.repository.ResetPasswordTokenRepository;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.holiday.repository.HolidaySyncStateRepository;
import com.ia.api.user.repository.AssetRepository;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.vacation.repository.VacationPeriodRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserRepository userRepository;
    private final VacationPeriodRepository vacationPeriodRepository;
    private final HolidaySyncStateRepository holidaySyncStateRepository;
    private final DayProfileRepository dayProfileRepository;
    private final AssetRepository assetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ResetPasswordTokenRepository resetPasswordTokenRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final ConsentRepository consentRepository;
    private final ProfilePhotoStorageService profilePhotoStorageService;
    private final PasswordEncoder passwordEncoder;

    public AccountDeletionService(
            UserRepository userRepository,
            VacationPeriodRepository vacationPeriodRepository,
            HolidaySyncStateRepository holidaySyncStateRepository,
            DayProfileRepository dayProfileRepository,
            AssetRepository assetRepository,
            RefreshTokenRepository refreshTokenRepository,
            ResetPasswordTokenRepository resetPasswordTokenRepository,
            ActivationTokenRepository activationTokenRepository,
            ConsentRepository consentRepository,
            ProfilePhotoStorageService profilePhotoStorageService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.vacationPeriodRepository = vacationPeriodRepository;
        this.holidaySyncStateRepository = holidaySyncStateRepository;
        this.dayProfileRepository = dayProfileRepository;
        this.assetRepository = assetRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetPasswordTokenRepository = resetPasswordTokenRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.consentRepository = consentRepository;
        this.profilePhotoStorageService = profilePhotoStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void deleteAccount(String email, String confirmPassword) {
        var user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(confirmPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Mot de passe invalide");
        }

        UUID userId = user.getId();

        // Delete dependent data in FK-safe order (all FK → users.id)
        vacationPeriodRepository.deleteAllByUserId(userId);
        holidaySyncStateRepository.deleteByUserId(userId);
        dayProfileRepository.deleteAllByUserId(userId);

        // Delete physical photo files before removing DB records
        assetRepository.findAllByUserId(userId).forEach(asset -> {
            try {
                profilePhotoStorageService.delete(asset.getStorageKey());
            } catch (Exception e) {
                log.warn("Could not delete asset file storageKey={} for userId={}", asset.getStorageKey(), userId, e);
            }
        });
        assetRepository.deleteAllByUserId(userId);

        refreshTokenRepository.deleteAllByUserId(userId);
        resetPasswordTokenRepository.deleteAllByUserId(userId);
        activationTokenRepository.deleteAllByUserId(userId);
        consentRepository.deleteAllByUserId(userId);

        userRepository.delete(user);

        log.info("Account permanently deleted userId={}", userId);
    }
}
