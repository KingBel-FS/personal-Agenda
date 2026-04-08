package com.ia.api.auth.service;

import com.ia.api.auth.domain.AccountStatus;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {
    @Mock
    private UserRepository userRepository;

    private AppUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AppUserDetailsService(userRepository);
    }

    @Test
    void returnsActiveUserDetails() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash");
        user.setAccountStatus(AccountStatus.ACTIVE);

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice@example.com");

        assertThat(details.getUsername()).isEqualTo("alice@example.com");
        assertThat(details.getPassword()).isEqualTo("hash");
    }

    @Test
    void rejectsPendingActivationUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash");
        user.setAccountStatus(AccountStatus.PENDING_ACTIVATION);

        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUserByUsername("alice@example.com"))
                .isInstanceOf(DisabledException.class)
                .hasMessage("Le compte n'est pas activé");
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("alice@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Utilisateur introuvable");
    }
}
