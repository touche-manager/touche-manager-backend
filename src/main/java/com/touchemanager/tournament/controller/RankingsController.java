package com.touchemanager.tournament.controller;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.shared.response.ApiResponse;
import com.touchemanager.tournament.dto.RankingEntryResponse;
import com.touchemanager.tournament.dto.TournamentResultResponse;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.Weapon;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.service.PouleService;
import com.touchemanager.tournament.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Results & Rankings — Public")
public class RankingsController {

    private final TournamentRepository tournamentRepository;
    private final PouleService pouleService;
    private final RankingService rankingService;

    // ── Historial de resultados ───────────────────────────────────────────────

    @GetMapping("/api/results")
    @Operation(summary = "Get finished tournament results list, filtered by category/gender/weapon/date")
    public ApiResponse<List<TournamentResultResponse>> getResults(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) Weapon weapon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        List<Tournament> tournaments = tournamentRepository.findFinishedTournaments(
                category, gender, weapon, dateFrom, dateTo);

        List<TournamentResultResponse> results = tournaments.stream()
                .map(t -> pouleService.getTournamentResults(t.getId()))
                .collect(Collectors.toList());

        return new ApiResponse<>(true, "Resultados obtenidos correctamente", results);
    }

    // Keep /api/rankings as a backward-compatible alias for the results list
    @GetMapping("/api/rankings")
    @Operation(summary = "[Alias] Same as /api/results — kept for backward compatibility")
    public ApiResponse<List<TournamentResultResponse>> getRankingsAlias(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) Weapon weapon,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return getResults(category, gender, weapon, dateFrom, dateTo);
    }

    // ── Rankings por puntos (RSP) ─────────────────────────────────────────────

    @GetMapping("/api/rankings/points")
    @Operation(summary = "Get accumulated rankings by discipline: last 4 tournaments, discard worst, sum best 3")
    public ApiResponse<List<RankingEntryResponse>> getRankingPoints(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) Weapon weapon) {

        List<RankingEntryResponse> rankings = rankingService.getRankings(category, gender, weapon);
        return new ApiResponse<>(true, "Rankings calculados correctamente", rankings);
    }
}
