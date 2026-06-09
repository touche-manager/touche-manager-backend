package com.touchemanager.tournament.dto;

import java.util.List;

/**
 * Response DTO for the public rankings endpoint.
 * Each entry represents one athlete's accumulated points from the last 4 tournaments
 * of a given discipline (category + gender + weapon), discarding the worst result.
 */
public record RankingEntryResponse(
        int position,
        Long athleteId,
        String fullName,
        String club,
        int totalPoints,
        int tournamentsPlayed,
        List<TournamentResult> tournaments
) {

    public record TournamentResult(
            Long tournamentId,
            String tournamentName,
            String date,
            boolean isNational,
            int placement,
            int basePoints,
            double coefficient,
            int finalPoints,
            boolean discarded
    ) {}
}
