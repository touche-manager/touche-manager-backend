package com.touchemanager.notification.controller;

import com.touchemanager.auth.service.JwtService;
import com.touchemanager.notification.sse.NotificationSseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE endpoint that streams real-time notifications to the authenticated user.
 *
 * <p>The JWT is passed as a query parameter because the browser's native
 * {@code EventSource} API does not support custom request headers.</p>
 *
 * <p>Endpoint: {@code GET /api/notifications/stream?token=<JWT>}</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationSseController {

    private final NotificationSseEmitterRegistry emitterRegistry;
    private final JwtService jwtService;

    @GetMapping(value = "/api/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time notifications (Server-Sent Events). Pass the JWT as ?token=")
    public SseEmitter streamNotifications(@RequestParam String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Invalid or expired JWT token");
        }
        Long userId = jwtService.extractUserId(token);
        return emitterRegistry.subscribe(userId);
    }
}
