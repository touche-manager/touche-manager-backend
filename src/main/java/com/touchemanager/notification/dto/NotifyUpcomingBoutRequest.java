package com.touchemanager.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotifyUpcomingBoutRequest {

    @NotNull(message = "minutesAhead is required")
    @Min(value = 1, message = "minutesAhead must be at least 1")
    @Max(value = 120, message = "minutesAhead must be at most 120")
    private Integer minutesAhead;

    @Size(max = 50, message = "Piste must be at most 50 characters")
    private String piste;
}
