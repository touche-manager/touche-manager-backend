package com.touchemanager.tournament.dto;

import com.touchemanager.tournament.entity.RefereeApplicationStatus;

import java.time.LocalDateTime;

public record RefereeApplicationResponse(
        Long id,
        Long tournamentId,
        String tournamentName,
        Long refereeId,
        String refereeName,
        String refereeEmail,
        RefereeApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime reviewedAt
) {}
