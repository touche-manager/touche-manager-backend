package com.touchemanager.bout.dto;

import com.touchemanager.bout.entity.BoutStatus;
import com.touchemanager.bout.entity.EliminationRound;

/**
 * Lightweight summary of a live (IN_PROGRESS) bout for the public spectator endpoint.
 */
public record LiveBoutSummary(
        Long boutId,
        Long tournamentId,
        String tournamentName,
        String piste,
        BoutStatus status,
        String athleteLeftName,
        String athleteRightName,
        int scoreLeft,
        int scoreRight,
        int elapsedSeconds,
        Long pouleId,
        Integer pouleNumber,
        EliminationRound eliminationRound
) {}
