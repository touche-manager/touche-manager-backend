package com.touchemanager.bout.dto;

import com.touchemanager.bout.entity.BoutFormat;
import com.touchemanager.bout.entity.BoutStatus;

import java.time.LocalDateTime;
import java.util.List;

public record BoutResponse(
        Long id,
        Long tournamentId,
        String tournamentName,
        BoutFormat format,
        BoutStatus status,
        AthleteSummary athleteLeft,
        AthleteSummary athleteRight,
        int scoreLeft,
        int scoreRight,
        int currentPeriod,
        int maxPeriods,
        int touchesTarget,
        int elapsedSeconds,
        Long winnerId,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<BoutEventResponse> events
) {

    public record AthleteSummary(
            Long id,
            String firstName,
            String lastName,
            String club
    ) {}
}
