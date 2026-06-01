package com.touchemanager.tournament.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(
        @NotNull(message = "Tournament ID cannot be null")
        Long tournamentId
) {}
