package com.touchemanager.bout.dto;

import com.touchemanager.bout.entity.EventSide;
import com.touchemanager.bout.entity.EventType;

import java.time.LocalDateTime;

public record BoutEventResponse(
        Long id,
        EventSide side,
        EventType eventType,
        int scoreDelta,
        LocalDateTime recordedAt
) {}
