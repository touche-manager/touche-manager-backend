package com.touchemanager.tournament.service;

import com.touchemanager.tournament.dto.TournamentResponse;
import java.util.List;

public interface TournamentService {
    List<TournamentResponse> getAvailableTournaments(String email);
    TournamentResponse getTournamentDetails(String email, Long tournamentId);
}
