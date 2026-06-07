package com.touchemanager.bout.dto;

import java.util.List;

public record TournamentStandingsResponse(
        Long tournamentId,
        String tournamentName,
        List<AthleteStanding> standings
) {

    public record AthleteStanding(
            int rank,
            Long athleteId,
            String firstName,
            String lastName,
            String club,
            int bouts,       // total bouts contested
            int victories,   // bouts won
            int defeats,     // bouts lost
            int touchesScored,    // V — touches given (points for)
            int touchesReceived,  // D — touches received (points against)
            int indicator         // touchesScored - touchesReceived (index)
    ) {}
}
