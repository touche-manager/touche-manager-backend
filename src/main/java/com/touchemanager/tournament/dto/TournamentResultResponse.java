package com.touchemanager.tournament.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for tournament final results.
 * Used by the public endpoint GET /api/tournaments/{id}/results.
 * Includes podium, standings, poule sheets, and elimination bracket.
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
        boolean isNational,
        List<Participant> participants,
        List<PodiumEntry> podium,
        List<FinalStanding> standings,
        List<PouleClassificationEntry> pouleClassification,
        List<PouleSheet> pouleSheets,
        BracketData bracket
) {

    /** Participant list entry — ordered by series number (ranking position at time of tournament) */
    public record Participant(
            int seriesNumber,
            Long athleteId,
            String fullName,
            String club
    ) {}

    /** Podium entry — top finishers (1st, 2nd, two 3rds) */
    public record PodiumEntry(
            int rank,
            Long athleteId,
            String fullName,
            String club
    ) {}

    /**
     * Final tournament standing — position only, no combat stats.
     * Stats belong to the poule phase (see PouleClassificationEntry).
     */
    public record FinalStanding(
            int rank,
            Long athleteId,
            String fullName,
            String club
    ) {}

    /**
     * Poule-phase classification row — stats from poule bouts only.
     * Ordered by poule rank (victories desc, indicator desc, TA desc).
     */
    public record PouleClassificationEntry(
            int rank,
            Long athleteId,
            String fullName,
            String club,
            int victories,
            int touchesScored,
            int touchesReceived,
            int indicator
    ) {}

    /** Full poule cross-table sheet */
    public record PouleSheet(
            int pouleNumber,
            List<PouleRow> rows
    ) {
        /** One row per fencer in the poule */
        public record PouleRow(
                int index,
                Long athleteId,
                String fullName,
                String club,
                /** Map of opponent index → cell value (e.g. "V5", "D3") */
                Map<Integer, String> cells,
                int victories,
                int touchesScored,
                int touchesReceived,
                int indicator,
                int rank
        ) {}
    }

    /** Full elimination bracket — bouts grouped by round */
    public record BracketData(
            /** Ordered list of rounds from earliest to FINAL */
            List<BracketRound> rounds
    ) {
        public record BracketRound(
                String round,
                String roundLabel,
                List<BracketBout> bouts
        ) {}

        public record BracketBout(
                Long boutId,
                int bracketPosition,
                String leftName,
                String rightName,
                int scoreLeft,
                int scoreRight,
                String winnerName,
                boolean finished,
                String status,
                String piste
        ) {}
    }
}
