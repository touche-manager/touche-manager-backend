package com.touchemanager.tournament.controller;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.shared.response.ApiResponse;
import com.touchemanager.tournament.dto.TournamentResultResponse;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.Weapon;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.service.PouleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
@Tag(name = "Rankings — Public")
public class RankingsController {

    private final TournamentRepository tournamentRepository;
    private final PouleService pouleService;

    @GetMapping
    @Operation(summary = "Get rankings: results of finished tournaments filtered by category, gender, weapon, and date range")
    public ApiResponse<List<TournamentResultResponse>> getRankings(
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

        return new ApiResponse<>(true, "Rankings obtenidos correctamente", results);
    }
}
