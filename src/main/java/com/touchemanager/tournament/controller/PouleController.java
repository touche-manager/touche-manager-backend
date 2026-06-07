package com.touchemanager.tournament.controller;

import com.touchemanager.shared.response.ApiResponse;
import com.touchemanager.tournament.dto.AssignRefereeRequest;
import com.touchemanager.tournament.dto.EliminationBracketResponse;
import com.touchemanager.tournament.dto.PouleResponse;
import com.touchemanager.tournament.dto.PouleStandingEntry;
import com.touchemanager.tournament.dto.TournamentResultResponse;
import com.touchemanager.tournament.service.PouleService;
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
@RequiredArgsConstructor
@Tag(name = "Poules")
public class PouleController {

    private final PouleService pouleService;

    // ── Organizer: generate poules ───────────────────────────────────────────

    @PostMapping("/api/organizer/tournaments/{tournamentId}/generate-poules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Generate poules for a tournament and transition it to POULES_IN_PROGRESS")
    public ApiResponse<List<PouleResponse>> generatePoules(
            @AuthenticationPrincipal String email,
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Poules generadas correctamente",
                pouleService.generatePoules(email, tournamentId));
    }

    // ── Organizer: generate elimination bracket ──────────────────────────────

    @PostMapping("/api/organizer/tournaments/{tournamentId}/generate-bracket")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Close poules, compute standings and generate the elimination bracket")
    public ApiResponse<EliminationBracketResponse> generateEliminationBracket(
            @AuthenticationPrincipal String email,
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Bracket de eliminatorias generado correctamente",
                pouleService.generateEliminationBracket(email, tournamentId));
    }

    // ── Common: list poules ──────────────────────────────────────────────────

    @GetMapping("/api/tournaments/{tournamentId}/poules")
    @PreAuthorize("hasAnyRole('REFEREE', 'ORGANIZER', 'ADMIN')")
    @Operation(summary = "List all poules for a tournament")
    public ApiResponse<List<PouleResponse>> getPoulesForTournament(
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Poules obtenidas correctamente",
                pouleService.getPoulesForTournament(tournamentId));
    }

    @GetMapping("/api/poules/{pouleId}")
    @PreAuthorize("hasAnyRole('REFEREE', 'ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get full details of a poule including bouts")
    public ApiResponse<PouleResponse> getPouleDetails(
            @AuthenticationPrincipal String email,
            org.springframework.security.core.Authentication authentication,
            @PathVariable Long pouleId) {
        // Organizers and Admins bypass the acceptance check; referees must be ACCEPTED
        boolean isRefereeOnly = authentication.getAuthorities().stream()
                .allMatch(a -> a.getAuthority().equals("ROLE_REFEREE"));
        String refereeEmail = isRefereeOnly ? email : null;
        return new ApiResponse<>(true, "Poule obtenida correctamente",
                pouleService.getPouleDetails(refereeEmail, pouleId));
    }

    // ── Organizer: assign referee to poule ───────────────────────────────────

    @PostMapping("/api/poules/{pouleId}/referees")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Assign a referee to a poule")
    public ApiResponse<PouleResponse> assignRefereeToPoule(
            @AuthenticationPrincipal String email,
            @PathVariable Long pouleId,
            @Valid @RequestBody AssignRefereeRequest request) {
        return new ApiResponse<>(true, "Arbitro asignado correctamente",
                pouleService.assignRefereeToPoule(email, pouleId, request));
    }

    @DeleteMapping("/api/poules/{pouleId}/referees/{refereeUserId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @Operation(summary = "Remove a referee from a poule")
    public ApiResponse<PouleResponse> removeRefereeFromPoule(
            @AuthenticationPrincipal String email,
            @PathVariable Long pouleId,
            @PathVariable Long refereeUserId) {
        return new ApiResponse<>(true, "Arbitro removido correctamente",
                pouleService.removeRefereeFromPoule(email, pouleId, refereeUserId));
    }

    // ── Referee: my poules ───────────────────────────────────────────────────

    @GetMapping("/api/poules/my/{tournamentId}")
    @PreAuthorize("hasAnyRole('REFEREE', 'ADMIN')")
    @Operation(summary = "Get poules assigned to the authenticated referee in a tournament")
    public ApiResponse<List<PouleResponse>> getRefereePoules(
            @AuthenticationPrincipal String email,
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Tus poules obtenidas correctamente",
                pouleService.getRefereePoules(email, tournamentId));
    }

    // ── Common: standings and bracket ────────────────────────────────────────

    @GetMapping("/api/tournaments/{tournamentId}/standings")
    @PreAuthorize("hasAnyRole('REFEREE', 'ORGANIZER', 'ADMIN')")
    @Operation(summary = "Get poule standings computed from all finished bouts")
    public ApiResponse<List<PouleStandingEntry>> getPouleStandings(
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Clasificacion de poules obtenida",
                pouleService.computePouleStandings(tournamentId));
    }

    @GetMapping("/api/tournaments/{tournamentId}/bracket")
    @PreAuthorize("hasAnyRole('REFEREE', 'ORGANIZER', 'ADMIN', 'ATHLETE')")
    @Operation(summary = "Get the elimination bracket for a tournament")
    public ApiResponse<EliminationBracketResponse> getEliminationBracket(
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Bracket de eliminatorias obtenido",
                pouleService.getEliminationBracket(tournamentId));
    }

    // ── Public: tournament results ───────────────────────────────────────────

    @GetMapping("/api/tournaments/{tournamentId}/results")
    @Operation(summary = "Get tournament results (podium + standings). Public endpoint.")
    public ApiResponse<TournamentResultResponse> getTournamentResults(
            @PathVariable Long tournamentId) {
        return new ApiResponse<>(true, "Resultados del torneo obtenidos",
                pouleService.getTournamentResults(tournamentId));
    }
}
