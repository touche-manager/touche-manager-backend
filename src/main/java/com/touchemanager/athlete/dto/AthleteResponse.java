package com.touchemanager.athlete.dto;

import com.touchemanager.athlete.entity.DominantHand;
import com.touchemanager.athlete.entity.Gender;

import java.time.LocalDate;

public record AthleteResponse(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String dni,
        LocalDate birthDate,
        Gender gender,
        DominantHand dominantHand,
        String club,
        String province
) {}
