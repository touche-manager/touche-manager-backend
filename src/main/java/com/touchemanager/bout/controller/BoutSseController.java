package com.touchemanager.bout.controller;

import com.touchemanager.bout.dto.BoutLiveUpdate;
import com.touchemanager.bout.service.BoutService;
import com.touchemanager.bout.sse.BoutSseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Tag(name = "Bout — Live Scoreboard (public SSE)")
public class BoutSseController {

    private final BoutSseEmitterRegistry emitterRegistry;
    private final BoutService boutService;

    @GetMapping(value = "/api/bouts/{id}/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live score updates of a bout (public, Server-Sent Events)")
    public SseEmitter streamBoutScore(@PathVariable Long id) {
        BoutLiveUpdate snapshot = boutService.getLiveSnapshot(id);
        SseEmitter emitter = emitterRegistry.subscribe(id);
        try {
            // Initial snapshot so the spectator sees the current score immediately
            emitter.send(SseEmitter.event().name("score-update").data(snapshot));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
