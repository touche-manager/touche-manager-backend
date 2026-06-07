package com.touchemanager.bout.dto;

import com.touchemanager.bout.entity.BoutFormat;
import com.touchemanager.bout.entity.BoutStatus;
import com.touchemanager.bout.entity.EliminationRound;
import com.touchemanager.bout.entity.EventSide;

import java.time.LocalDateTime;
import java.util.List;

public record BoutResponse(
        Long id,
        Long tournamentId,
        String tournamentName,
        Long pouleId,
        Integer pouleNumber,
        Integer boutOrder,
        BoutFormat format,
        BoutStatus status,
        AthleteSummary athleteLeft,
        AthleteSummary athleteRight,  // null = BYE
        int scoreLeft,
        int scoreRight,
        int currentPeriod,
        int maxPeriods,
        int touchesTarget,
        int elapsedSeconds,
        Long winnerId,
        EliminationRound eliminationRound,
        Integer bracketPosition,
        EventSide priority,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<BoutEventResponse> events,
        List<RefereeInfo> referees
) {

    public record AthleteSummary(
            Long id,
            String firstName,
            String lastName,
            String club
    ) {}

    public record RefereeInfo(
            Long userId,
            String fullName,
            String email
    ) {}
}
