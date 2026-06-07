package com.touchemanager.tournament.service;

import com.touchemanager.tournament.dto.RefereeApplicationResponse;
import com.touchemanager.tournament.dto.ReviewApplicationRequest;

import java.util.List;

public interface RefereeApplicationService {
    RefereeApplicationResponse apply(String refereeEmail, Long tournamentId);
    List<RefereeApplicationResponse> getApplicationsForTournament(String organizerEmail, Long tournamentId);
    List<RefereeApplicationResponse> getMyApplications(String refereeEmail);
    RefereeApplicationResponse reviewApplication(String organizerEmail, Long applicationId, ReviewApplicationRequest request);
    void cancelApplication(String refereeEmail, Long applicationId);
}
