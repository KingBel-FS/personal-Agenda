package com.ia.api.sync.service;

import com.ia.api.sync.api.SyncEventResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeSyncService {

    private final Map<String, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String email) {
        String key = normalize(email);
        SseEmitter emitter = new SseEmitter(0L);
        emittersByUser.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(ignored -> removeEmitter(key, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(new SyncEventResponse("CONNECTED", Instant.now().toString())));
        } catch (IOException exception) {
            removeEmitter(key, emitter);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public void publish(String email, String scope) {
        String key = normalize(email);
        List<SseEmitter> emitters = emittersByUser.getOrDefault(key, List.of());
        if (emitters.isEmpty()) {
            return;
        }

        SyncEventResponse payload = new SyncEventResponse(scope, Instant.now().toString());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("sync").data(payload));
            } catch (IOException exception) {
                removeEmitter(key, emitter);
                emitter.completeWithError(exception);
            }
        }
    }

    private void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(key);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(key);
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
