package com.touchemanager.auth.dto;

public record AdminStatsResponse(
        long totalUsers,
        long totalAthletes,
        long totalTournaments,
        long activeTournaments,
        long finishedTournaments,
        long totalEnrollments,
        long pendingDocuments
) {}
