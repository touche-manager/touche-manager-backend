package com.touchemanager.bout.controller;

import com.touchemanager.bout.dto.BoutEventRequest;
import com.touchemanager.bout.dto.BoutRequest;
import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.bout.dto.LiveBoutSummary;
import com.touchemanager.bout.dto.ElapsedTimeRequest;
import com.touchemanager.bout.dto.PisteRequest;
import com.touchemanager.bout.dto.TournamentStandingsResponse;
import com.touchemanager.bout.entity.EventSide;
import com.touchemanager.bout.service.BoutService;
import com.touchemanager.shared.response.ApiResponse;
import com.touchemanager.tournament.dto.AssignRefereeRequest;
import com.touchemanager.tournament.dto.OrganizerTournamentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bouts")
@RequiredArgsConstructor
@Tag(name = "Bout — Referee Scoring")
public class BoutController {

    private final BoutService boutService;

    @GetMapping("/tournaments")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN', 'ORGANIZER')")
    @Operation(summary = "List all tournaments (referee view, no ownership check)")
    public ApiResponse<List<OrganizerTournamentResponse>> getAllTournaments() {
        return new ApiResponse<>(true, "Tournaments retrieved successfully",
                boutService.getAllTournaments());
    }

    @GetMapping("/live")
    @Operation(summary = "List all currently in-progress bouts — public endpoint for spectators")
    public ApiResponse<List<LiveBoutSummary>> getLiveBouts() {
        return new ApiResponse<>(true, "Asaltos en vivo obtenidos correctamente",
                boutService.getLiveBouts());
    }

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
    @Operation(summary = "Update elapsed time and timer state for an active bout (sent on pause/resume)")
    public ApiResponse<BoutResponse> updateElapsedTime(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId,
            @Valid @RequestBody ElapsedTimeRequest request) {
        return new ApiResponse<>(true, "Elapsed time updated successfully",
                boutService.updateTimerState(email, boutId, request.getElapsedSeconds(), request.isTimerPaused(), request.getCurrentPeriod()));
    }

    @PatchMapping("/{boutId}/piste")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Assign the piste (strip) where the bout takes place")
    public ApiResponse<BoutResponse> updatePiste(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId,
            @Valid @RequestBody PisteRequest request) {
        return new ApiResponse<>(true, "Pista asignada correctamente",
                boutService.updatePiste(email, boutId, request.getPiste()));
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

    @PostMapping("/{boutId}/priority")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Assign priority to one side (LEFT/RIGHT) for tie-breaking")
    public ApiResponse<BoutResponse> assignPriority(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId,
            @RequestBody Map<String, String> body) {
        EventSide side = EventSide.valueOf(body.get("side"));
        return new ApiResponse<>(true, "Prioridad asignada correctamente",
                boutService.assignPriority(email, boutId, side));
    }

    @PostMapping("/{boutId}/referees")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Assign a referee to an elimination bout")
    public ApiResponse<BoutResponse> assignRefereeToEliminationBout(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId,
            @Valid @RequestBody AssignRefereeRequest request) {
        return new ApiResponse<>(true, "Arbitro asignado al asalto correctamente",
                boutService.assignRefereeToEliminationBout(email, boutId, request));
    }

    @DeleteMapping("/{boutId}/referees/{refereeId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Remove a referee from an elimination bout")
    public ApiResponse<BoutResponse> removeRefereeFromEliminationBout(
            @AuthenticationPrincipal String email,
            @PathVariable Long boutId,
            @PathVariable Long refereeId) {
        return new ApiResponse<>(true, "Arbitro removido del asalto correctamente",
                boutService.removeRefereeFromEliminationBout(email, boutId, refereeId));
    }

    @GetMapping("/tournament/{tournamentId}/my-bouts")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Get bouts assigned to the authenticated referee in a tournament")
    public ApiResponse<List<BoutResponse>> getMyBouts(
            @AuthenticationPrincipal String email,
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Tus asaltos obtenidos correctamente",
                boutService.getMyAssignedBouts(email, tournamentId));
    }

    @GetMapping("/elimination/{tournamentId}")
    @PreAuthorize("hasAnyRole('REFEREE', 'ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get all elimination bouts for a tournament")
    public ApiResponse<List<BoutResponse>> getEliminationBouts(
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Asaltos de eliminatoria obtenidos",
                boutService.getEliminationBouts(tournamentId));
    }

    @GetMapping("/my-elimination/{tournamentId}")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Get elimination bouts assigned to the authenticated referee")
    public ApiResponse<List<BoutResponse>> getMyEliminationBouts(
            @AuthenticationPrincipal String email,
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Tus asaltos de eliminatoria obtenidos",
                boutService.getMyEliminationBouts(email, tournamentId));
    }
}
