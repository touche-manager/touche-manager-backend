package com.touchemanager.bout.service;

import com.touchemanager.bout.dto.BoutEventRequest;
import com.touchemanager.bout.dto.BoutLiveUpdate;
import com.touchemanager.bout.dto.BoutRequest;
import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.bout.dto.LiveBoutSummary;
import com.touchemanager.bout.dto.TournamentStandingsResponse;
import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.entity.EventSide;
import com.touchemanager.tournament.dto.AssignRefereeRequest;
import com.touchemanager.tournament.dto.OrganizerTournamentResponse;

import java.util.List;

public interface BoutService {

    List<OrganizerTournamentResponse> getAllTournaments();

    BoutResponse createBout(String email, BoutRequest request);

    BoutResponse startBout(String email, Long boutId);

    BoutResponse recordEvent(String email, Long boutId, BoutEventRequest request);

    BoutResponse updateElapsedTime(String email, Long boutId, int elapsedSeconds);

    /** Update elapsed time AND timer state (paused/running) — broadcasts pause to spectators */
    BoutResponse updateTimerState(String email, Long boutId, int elapsedSeconds, boolean timerPaused, Integer currentPeriod);

    BoutResponse finishBout(String email, Long boutId);

    /** Assign priority to one side of the bout (used for tie-breaking) */
    BoutResponse assignPriority(String email, Long boutId, EventSide side);

    List<BoutResponse> getBoutsByTournament(Long tournamentId);

    BoutResponse getBoutDetails(Long boutId);

    TournamentStandingsResponse getTournamentStandings(Long tournamentId);

    BoutResponse assignRefereeToEliminationBout(String organizerEmail, Long boutId, AssignRefereeRequest request);

    List<BoutResponse> getEliminationBouts(Long tournamentId);

    List<BoutResponse> getMyEliminationBouts(String refereeEmail, Long tournamentId);

    List<BoutResponse> getMyAssignedBouts(String refereeEmail, Long tournamentId);

    BoutResponse removeRefereeFromEliminationBout(String organizerEmail, Long boutId, Long refereeUserId);

    /** Advance the winner of a finished elimination bout to the next round */
    void advanceEliminationWinner(Bout finishedBout);

    /** Assign the piste (strip) where the bout will take place */
    BoutResponse updatePiste(String email, Long boutId, String piste);

    /** Current state of a bout for public live-scoreboard subscribers */
    BoutLiveUpdate getLiveSnapshot(Long boutId);

    /** All currently IN_PROGRESS bouts — public endpoint for spectators */
    List<LiveBoutSummary> getLiveBouts();
}
