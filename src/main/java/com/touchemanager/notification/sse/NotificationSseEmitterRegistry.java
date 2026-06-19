package com.touchemanager.notification.sse;

import com.touchemanager.notification.dto.NotificationDTO;
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
 * Keeps track of active SSE connections per user so notifications
 * can be pushed in real time to any authenticated user.
 *
 * <p>A user may have more than one open tab, so we track a list of
 * emitters per userId.</p>
 */
@Component
public class NotificationSseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(NotificationSseEmitterRegistry.class);
    private static final long EMITTER_TIMEOUT_MS = 60 * 60 * 1000L; // 1 hour

    private final Map<Long, List<SseEmitter>> activeEmitters = new ConcurrentHashMap<>();

    /**
     * Register a new SSE connection for the given user.
     * Returns the emitter that the controller must return to the browser.
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> emitters = activeEmitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(()    -> remove(userId, emitter));
        emitter.onError(e       -> remove(userId, emitter));

        log.info("New SSE subscriber for user {} ({} active connections)", userId, emitters.size());
        return emitter;
    }

    /**
     * Push a notification to every open tab/connection for the given user.
     * Silently ignores users who are not currently connected.
     */
    public void send(Long userId, NotificationDTO notification) {
        List<SseEmitter> emitters = activeEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE connections for user {}, notification will be fetched on next load", userId);
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException | IllegalStateException e) {
                remove(userId, emitter);
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = activeEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                activeEmitters.remove(userId, emitters);
            }
        }
        log.debug("SSE emitter removed for user {}", userId);
    }
}
