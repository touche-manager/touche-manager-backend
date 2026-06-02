package com.touchemanager.bout.service;

import com.touchemanager.bout.dto.BoutEventRequest;
import com.touchemanager.bout.dto.BoutRequest;
import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.bout.dto.TournamentStandingsResponse;
import com.touchemanager.tournament.dto.OrganizerTournamentResponse;

import java.util.List;

public interface BoutService {

    List<OrganizerTournamentResponse> getAllTournaments();

    BoutResponse createBout(String email, BoutRequest request);

    BoutResponse startBout(String email, Long boutId);

    BoutResponse recordEvent(String email, Long boutId, BoutEventRequest request);

    BoutResponse updateElapsedTime(String email, Long boutId, int elapsedSeconds);

    BoutResponse finishBout(String email, Long boutId);

    List<BoutResponse> getBoutsByTournament(Long tournamentId);

    BoutResponse getBoutDetails(Long boutId);

    TournamentStandingsResponse getTournamentStandings(Long tournamentId);
}
