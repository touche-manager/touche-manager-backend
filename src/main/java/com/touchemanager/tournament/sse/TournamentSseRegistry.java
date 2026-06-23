package com.touchemanager.tournament.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps track of active SSE connections per tournament so that organizers and
 * spectators can receive live "refresh" signals whenever the tournament state
 * changes (bout score update, poule started, bracket generated, etc.).
 */
@Component
public class TournamentSseRegistry {

    private static final Logger log = LoggerFactory.getLogger(TournamentSseRegistry.class);
    private static final long EMITTER_TIMEOUT_MS = 60 * 60 * 1000L; // 60 minutes

    private final Map<Long, List<SseEmitter>> activeEmitters = new ConcurrentHashMap<>();

    /** Register a new subscriber for the given tournament */
    public SseEmitter subscribe(Long tournamentId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> emitters = activeEmitters.computeIfAbsent(tournamentId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(tournamentId, emitter));
        emitter.onTimeout(() -> remove(tournamentId, emitter));
        emitter.onError(e -> remove(tournamentId, emitter));

        log.info("New SSE subscriber for tournament {} ({} active)", tournamentId, emitters.size());
        return emitter;
    }

    /**
     * Broadcast a refresh signal to every subscriber for the given tournament.
     * The payload is simply the tournamentId so clients know which resource to reload.
     */
    public void broadcast(Long tournamentId) {
        List<SseEmitter> emitters = activeEmitters.get(tournamentId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name("refresh").data(tournamentId));
            } catch (IOException | IllegalStateException e) {
                remove(tournamentId, emitter);
            }
        }
        log.debug("Broadcasted refresh for tournament {} to {} subscriber(s)", tournamentId, emitters.size());
    }

    private void remove(Long tournamentId, SseEmitter emitter) {
        List<SseEmitter> emitters = activeEmitters.get(tournamentId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                activeEmitters.remove(tournamentId, emitters);
            }
        }
    }
}
