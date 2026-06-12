package com.touchemanager.tournament.service;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.dto.PublicTournamentResponse;
import com.touchemanager.tournament.dto.TournamentResponse;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.TournamentPhase;
import com.touchemanager.tournament.entity.Weapon;

import java.time.LocalDate;
import java.util.List;

public interface TournamentService {
    List<TournamentResponse> getAvailableTournaments(String email);
    TournamentResponse getTournamentDetails(String email, Long tournamentId);

    /** Public tournament listing with optional filters — no auth required */
    List<PublicTournamentResponse> searchPublicTournaments(
            TournamentPhase status, Weapon weapon, Category category, Gender gender,
            LocalDate dateFrom, LocalDate dateTo);
}
