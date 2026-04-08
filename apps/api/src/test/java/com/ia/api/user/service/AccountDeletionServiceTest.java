package com.ia.api.user.service;

import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.ActivationTokenRepository;
import com.ia.api.auth.repository.ConsentRepository;
import com.ia.api.auth.repository.RefreshTokenRepository;
import com.ia.api.auth.repository.ResetPasswordTokenRepository;
import com.ia.api.auth.repository.UserRepository;
import com.ia.api.holiday.repository.HolidaySyncStateRepository;
import com.ia.api.user.domain.AssetEntity;
import com.ia.api.user.repository.AssetRepository;
import com.ia.api.user.repository.DayProfileRepository;
import com.ia.api.vacation.repository.VacationPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountDeletionServiceTest {

    private UserRepository userRepository;
    private VacationPeriodRepository vacationPeriodRepository;
    private HolidaySyncStateRepository holidaySyncStateRepository;
    private DayProfileRepository dayProfileRepository;
    private AssetRepository assetRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private ResetPasswordTokenRepository resetPasswordTokenRepository;
    private ActivationTokenRepository activationTokenRepository;
    private ConsentRepository consentRepository;
    private ProfilePhotoStorageService profilePhotoStorageService;
    private PasswordEncoder passwordEncoder;
    private AccountDeletionService service;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        vacationPeriodRepository = Mockito.mock(VacationPeriodRepository.class);
        holidaySyncStateRepository = Mockito.mock(HolidaySyncStateRepository.class);
        dayProfileRepository = Mockito.mock(DayProfileRepository.class);
        assetRepository = Mockito.mock(AssetRepository.class);
        refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        resetPasswordTokenRepository = Mockito.mock(ResetPasswordTokenRepository.class);
        activationTokenRepository = Mockito.mock(ActivationTokenRepository.class);
        consentRepository = Mockito.mock(ConsentRepository.class);
        profilePhotoStorageService = Mockito.mock(ProfilePhotoStorageService.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);

        service = new AccountDeletionService(
                userRepository,
                vacationPeriodRepository,
                holidaySyncStateRepository,
                dayProfileRepository,
                assetRepository,
                refreshTokenRepository,
                resetPasswordTokenRepository,
                activationTokenRepository,
                consentRepository,
                profilePhotoStorageService,
                passwordEncoder
        );
    }

    @Test
    void deleteAccountRemovesAllUserDataAndDeletesUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserEntity user = userWithPassword(userId, "hashed-password");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed-password")).thenReturn(true);

        AssetEntity asset = new AssetEntity();
        asset.setStorageKey("u/photo.png");
        when(assetRepository.findAllByUserId(userId)).thenReturn(List.of(asset));

        service.deleteAccount("alice@example.com", "secret");

        verify(vacationPeriodRepository).deleteAllByUserId(userId);
        verify(holidaySyncStateRepository).deleteByUserId(userId);
        verify(dayProfileRepository).deleteAllByUserId(userId);
        verify(profilePhotoStorageService).delete("u/photo.png");
        verify(assetRepository).deleteAllByUserId(userId);
        verify(refreshTokenRepository).deleteAllByUserId(userId);
        verify(resetPasswordTokenRepository).deleteAllByUserId(userId);
        verify(activationTokenRepository).deleteAllByUserId(userId);
        verify(consentRepository).deleteAllByUserId(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAccountThrowsBadCredentialsWhenPasswordIsWrong() {
        UUID userId = UUID.randomUUID();
        UserEntity user = userWithPassword(userId, "hashed-password");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> service.deleteAccount("alice@example.com", "wrong"));

        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteAccountThrowsWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.deleteAccount("ghost@example.com", "pass"));

        verify(userRepository, never()).delete(any());
    }

    private UserEntity userWithPassword(UUID id, String passwordHash) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("alice@example.com");
        user.setPasswordHash(passwordHash);
        return user;
    }
}
