package com.touchemanager.bout.dto;

import com.touchemanager.bout.entity.EventSide;
import com.touchemanager.bout.entity.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoutEventRequest {

    @NotNull(message = "Side is required (LEFT or RIGHT)")
    private EventSide side;

    @NotNull(message = "Event type is required")
    private EventType eventType;
}
