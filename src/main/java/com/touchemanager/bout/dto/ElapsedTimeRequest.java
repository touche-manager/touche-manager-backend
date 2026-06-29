package com.touchemanager.bout.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElapsedTimeRequest {

    @NotNull(message = "Elapsed seconds is required")
    @Min(value = 0, message = "Elapsed seconds must be non-negative")
    private Integer elapsedSeconds;

    /** True when the referee is pausing the clock, false when resuming */
    private boolean timerPaused = false;

    private Integer currentPeriod;
}
