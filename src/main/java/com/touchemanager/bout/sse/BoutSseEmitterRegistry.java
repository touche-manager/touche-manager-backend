package com.touchemanager.bout.sse;

import com.touchemanager.bout.dto.BoutLiveUpdate;
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
 * Keeps track of the active SSE connections per bout so the score
 * can be pushed to public spectators in real time.
 */
@Component
public class BoutSseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(BoutSseEmitterRegistry.class);
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private final Map<Long, List<SseEmitter>> activeEmitters = new ConcurrentHashMap<>();

    /** Register a new spectator connection for the given bout */
    public SseEmitter subscribe(Long boutId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> emitters = activeEmitters.computeIfAbsent(boutId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(boutId, emitter));
        emitter.onTimeout(() -> remove(boutId, emitter));
        emitter.onError(e -> remove(boutId, emitter));

        log.info("New SSE subscriber for bout {} ({} active)", boutId, emitters.size());
        return emitter;
    }

    /** Push a score update to every spectator subscribed to the bout */
    public void broadcast(Long boutId, BoutLiveUpdate update) {
        List<SseEmitter> emitters = activeEmitters.get(boutId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("score-update").data(update));
            } catch (IOException | IllegalStateException e) {
                remove(boutId, emitter);
            }
        }
    }

    private void remove(Long boutId, SseEmitter emitter) {
        List<SseEmitter> emitters = activeEmitters.get(boutId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                activeEmitters.remove(boutId, emitters);
            }
        }
    }
}
