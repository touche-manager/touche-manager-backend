package com.touchemanager.athlete.service;

import com.touchemanager.athlete.dto.AthleteRequest;
import com.touchemanager.athlete.dto.AthleteResponse;

public interface AthleteService {

    AthleteResponse getProfile(String email);

    AthleteResponse createProfile(String email, AthleteRequest request);

    AthleteResponse updateProfile(String email, AthleteRequest request);
}
