package com.touchemanager.tournament.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRefereeRequest(@NotNull Long refereeUserId) {}
