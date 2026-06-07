package com.touchemanager.tournament.dto;

import java.util.List;

/**
 * Response DTO for tournament final results.
 * Used by the public endpoint GET /api/tournaments/{id}/results.
 */
public record TournamentResultResponse(
        Long tournamentId,
        String name,
        String weapon,
        String category,
        String gender,
        String location,
        String date,
        String phase,
        List<PodiumEntry> podium,
        List<FinalStanding> standings
) {
    /** Podium entry — top finishers (1st, 2nd, two 3rds) */
    public record PodiumEntry(
            int rank,
            Long athleteId,
            String fullName,
            String club
    ) {}

    /** Full standings row for the final results table */
    public record FinalStanding(
            int rank,
            Long athleteId,
            String fullName,
            String club,
            int bouts,
            int victories,
            int defeats,
            int touchesScored,
            int touchesReceived,
            int indicator
    ) {}
}
