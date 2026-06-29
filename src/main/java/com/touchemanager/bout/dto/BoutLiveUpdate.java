package com.touchemanager.bout.dto;

import com.touchemanager.bout.entity.BoutStatus;

/** Payload pushed to public SSE subscribers watching a live bout */
public record BoutLiveUpdate(
        Long boutId,
        int scoreLeft,
        int scoreRight,
        String leftName,
        String rightName,
        BoutStatus status,
        int elapsedSeconds,
        int period,
        String piste,
        String winnerName,
        boolean timerRunning
) {}
