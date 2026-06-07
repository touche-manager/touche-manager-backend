package com.touchemanager.tournament.service;

import com.touchemanager.tournament.dto.AssignRefereeRequest;
import com.touchemanager.tournament.dto.EliminationBracketResponse;
import com.touchemanager.tournament.dto.PouleResponse;
import com.touchemanager.tournament.dto.PouleStandingEntry;
import com.touchemanager.tournament.dto.TournamentResultResponse;

import java.util.List;

public interface PouleService {
    /** Generate poules for the tournament and transition it to POULES_IN_PROGRESS */
    List<PouleResponse> generatePoules(String organizerEmail, Long tournamentId);

    List<PouleResponse> getPoulesForTournament(Long tournamentId);

    PouleResponse getPouleDetails(String refereeEmail, Long pouleId);

    PouleResponse assignRefereeToPoule(String organizerEmail, Long pouleId, AssignRefereeRequest request);

    PouleResponse removeRefereeFromPoule(String organizerEmail, Long pouleId, Long refereeUserId);

    /** Returns poules assigned to the authenticated referee in a given tournament */
    List<PouleResponse> getRefereePoules(String refereeEmail, Long tournamentId);

    /** Compute standings from finished bouts across all poules */
    List<PouleStandingEntry> computePouleStandings(Long tournamentId);

    /** Generate elimination bracket and transition tournament to ELIMINATION_IN_PROGRESS */
    EliminationBracketResponse generateEliminationBracket(String organizerEmail, Long tournamentId);

    EliminationBracketResponse getEliminationBracket(Long tournamentId);

    /** Get final tournament results (podium + full standings). Works for any phase. */
    TournamentResultResponse getTournamentResults(Long tournamentId);
}
