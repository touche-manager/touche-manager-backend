package com.touchemanager.tournament.service;

import com.touchemanager.tournament.dto.DocumentValidationRequest;
import com.touchemanager.tournament.dto.EnrollmentDetailResponse;
import com.touchemanager.tournament.dto.OrganizerTournamentResponse;
import com.touchemanager.tournament.dto.TournamentRequest;

import java.util.List;

public interface OrganizerTournamentService {

    OrganizerTournamentResponse createTournament(String email, TournamentRequest request);

    OrganizerTournamentResponse updateTournament(String email, Long tournamentId, TournamentRequest request);

    void deleteTournament(String email, Long tournamentId);

    List<OrganizerTournamentResponse> getMyTournaments(String email);

    OrganizerTournamentResponse getTournamentById(String email, Long tournamentId);

    List<EnrollmentDetailResponse> getTournamentEnrollments(String email, Long tournamentId);

    void validateDocument(String email, Long documentId, DocumentValidationRequest request);
}
