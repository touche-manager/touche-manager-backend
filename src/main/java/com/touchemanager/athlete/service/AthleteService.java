package com.touchemanager.athlete.service;

import com.touchemanager.athlete.dto.AthleteBoutResponse;
import com.touchemanager.athlete.dto.AthleteRequest;
import com.touchemanager.athlete.dto.AthleteResponse;
import com.touchemanager.bout.entity.BoutStatus;

import java.util.List;

public interface AthleteService {

    AthleteResponse getProfile(String email);

    AthleteResponse createProfile(String email, AthleteRequest request);

    AthleteResponse updateProfile(String email, AthleteRequest request);

    /** Historical bouts of the authenticated athlete, optionally filtered */
    List<AthleteBoutResponse> getMyBouts(String email, Long tournamentId, BoutStatus status);
}
