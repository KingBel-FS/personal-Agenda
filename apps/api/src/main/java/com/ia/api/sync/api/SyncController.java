package com.ia.api.sync.api;

import com.ia.api.auth.service.AppUserDetailsService;
import com.ia.api.auth.service.JwtService;
import com.ia.api.sync.service.RealtimeSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final RealtimeSyncService realtimeSyncService;

    public SyncController(
            JwtService jwtService,
            AppUserDetailsService userDetailsService,
            RealtimeSyncService realtimeSyncService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.realtimeSyncService = realtimeSyncService;
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("access_token") String accessToken) {
        String email;
        try {
            email = jwtService.extractUsername(accessToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtService.isTokenValid(accessToken, userDetails.getUsername())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
            }
        } catch (Exception exception) {
            if (exception instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }

        return realtimeSyncService.subscribe(email);
    }
}
