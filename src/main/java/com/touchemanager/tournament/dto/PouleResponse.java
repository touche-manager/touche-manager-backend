package com.touchemanager.tournament.dto;

import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.tournament.entity.PouleStatus;

import java.util.List;

public record PouleResponse(
        Long id,
        Long tournamentId,
        String tournamentName,
        int number,
        PouleStatus status,
        List<PouleAthleteInfo> athletes,
        List<PouleRefereeInfo> referees,
        List<BoutResponse> bouts,
        int totalBouts,
        int finishedBouts
) {
    public record PouleAthleteInfo(Long id, String fullName, String club) {}
    public record PouleRefereeInfo(Long userId, String fullName, String email) {}
}
