package com.ia.api.auth.service;

import com.ia.api.auth.domain.AccountStatus;
import com.ia.api.auth.domain.UserEntity;
import com.ia.api.auth.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Le compte n'est pas activé");
        }

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(AuthorityUtils.NO_AUTHORITIES)
                .build();
    }
}
