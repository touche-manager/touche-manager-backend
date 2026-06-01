package com.touchemanager.bout.controller;

import com.touchemanager.bout.dto.BoutEventRequest;
import com.touchemanager.bout.dto.BoutRequest;
import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.bout.dto.ElapsedTimeRequest;
import com.touchemanager.bout.dto.TournamentStandingsResponse;
import com.touchemanager.bout.service.BoutService;
import com.touchemanager.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bouts")
@RequiredArgsConstructor
@Tag(name = "Bout — Referee Scoring")
public class BoutController {

    private final BoutService boutService;

    @GetMapping("/tournament/{tournamentId}")
    @Operation(summary = "Get all bouts for a tournament")
    public ApiResponse<List<BoutResponse>> getBoutsByTournament(@PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Bouts retrieved successfully",
                boutService.getBoutsByTournament(tournamentId));
    }

    @GetMapping("/{boutId}")
    @Operation(summary = "Get details of a specific bout")
    public ApiResponse<BoutResponse> getBoutDetails(@PathVariable Long boutId) {
        return new ApiResponse<>(true, "Bout retrieved successfully",
                boutService.getBoutDetails(boutId));
    }

    @GetMapping("/tournament/{tournamentId}/standings")
    @Operation(summary = "Get tournament standings calculated from all finished bouts")
    public ApiResponse<TournamentStandingsResponse> getTournamentStandings(
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Standings calculated successfully",
                boutService.getTournamentStandings(tournamentId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Create a new bout between two athletes")
    public ApiResponse<BoutResponse> createBout(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody BoutRequest request) {
        return new ApiResponse<>(true, "Bout created successfully",
                boutService.createBout(email, request));
    }

    @PostMapping("/{boutId}/start")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Start a pending bout")
    public ApiResponse<BoutResponse> startBout(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId) {
        return new ApiResponse<>(true, "Bout started successfully",
                boutService.startBout(email, boutId));
    }

    @PostMapping("/{boutId}/events")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Record a scoring event (touche, penalty, card) in an active bout")
    public ApiResponse<BoutResponse> recordEvent(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId,
            @Valid @RequestBody BoutEventRequest request) {
        return new ApiResponse<>(true, "Event recorded successfully",
                boutService.recordEvent(email, boutId, request));
    }

    @PatchMapping("/{boutId}/time")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Update elapsed time for an active bout (sent periodically from frontend timer)")
    public ApiResponse<BoutResponse> updateElapsedTime(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId,
            @Valid @RequestBody ElapsedTimeRequest request) {
        return new ApiResponse<>(true, "Elapsed time updated successfully",
                boutService.updateElapsedTime(email, boutId, request.getElapsedSeconds()));
    }

    @PostMapping("/{boutId}/finish")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Finish an active or pending bout and declare the winner")
    public ApiResponse<BoutResponse> finishBout(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId) {
        return new ApiResponse<>(true, "Bout finished successfully",
                boutService.finishBout(email, boutId));
    }
}
