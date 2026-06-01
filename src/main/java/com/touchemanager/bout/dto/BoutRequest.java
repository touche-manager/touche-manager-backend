package com.touchemanager.bout.dto;

import com.touchemanager.bout.entity.BoutFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoutRequest {

    @NotNull(message = "Tournament ID is required")
    private Long tournamentId;

    @NotNull(message = "Left athlete ID is required")
    private Long athleteLeftId;

    @NotNull(message = "Right athlete ID is required")
    private Long athleteRightId;

    @NotNull(message = "Bout format is required")
    private BoutFormat format;
}
