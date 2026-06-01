package com.touchemanager.tournament.dto;

import com.touchemanager.tournament.entity.EnrollmentStatus;
import java.math.BigDecimal;

public record EnrollmentResponse(
        Long id,
        Long athleteId,
        Long tournamentId,
        String tournamentName,
        BigDecimal amount,
        EnrollmentStatus status,
        String paymentLink
) {}
