package com.touchemanager.tournament.dto;

public record PouleStandingEntry(
        Long athleteId,
        String fullName,
        String club,
        int pouleNumber,
        int victories,
        int touchesScored,
        int touchesReceived,
        int indicator   // touchesScored - touchesReceived
) {}
