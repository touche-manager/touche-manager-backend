package com.touchemanager.tournament.dto;

import com.touchemanager.tournament.entity.RefereeApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewApplicationRequest(
        @NotNull RefereeApplicationStatus status
) {}
