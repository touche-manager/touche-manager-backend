package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.tournament.dto.RankingEntryResponse;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.Weapon;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.service.PouleService;
import com.touchemanager.tournament.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final TournamentRepository tournamentRepository;
    private final PouleService pouleService;

    // ── Official point table — Reglamento de Selección 2026 ──────────────────
    private static final NavigableMap<Integer, Integer> POINTS_TABLE = new TreeMap<>();
    static {
        POINTS_TABLE.put(1,  32);
        POINTS_TABLE.put(2,  26);
        POINTS_TABLE.put(3,  22);
        POINTS_TABLE.put(5,  16);
        POINTS_TABLE.put(6,  15);
        POINTS_TABLE.put(7,  14);
        POINTS_TABLE.put(8,  13);
        POINTS_TABLE.put(9,   8);
        POINTS_TABLE.put(13,  4);
        POINTS_TABLE.put(17,  2);
        POINTS_TABLE.put(33,  1);
        POINTS_TABLE.put(65,  0); // sentinel — beyond 64th place
    }

    private static final double NATIONAL_COEFFICIENT = 1.2;
    private static final double DEFAULT_COEFFICIENT  = 1.0;
    private static final int    MAX_TOURNAMENTS      = 4;
    private static final int    TOURNAMENTS_COUNTED  = 3; // best 3 of 4 (worst discarded)

    @Override
    @Transactional(readOnly = true)
    public List<RankingEntryResponse> getRankings(Category category, Gender gender, Weapon weapon) {
        // 1. Fetch the last MAX_TOURNAMENTS finished tournaments for this discipline
        List<Tournament> tournaments = tournamentRepository
                .findTop4ByDisciplineOrderByDateDesc(category, gender, weapon);

        if (tournaments.isEmpty()) return List.of();

        // 2. Compute results for each tournament and collect per-athlete data
        //    athleteResults: athleteId → list of TournamentResult (one per tournament)
        Map<Long, List<RankingEntryResponse.TournamentResult>> athleteResults = new LinkedHashMap<>();
        Map<Long, String> athleteNames = new HashMap<>();
        Map<Long, String> athleteClubs = new HashMap<>();

        for (Tournament t : tournaments) {
            var results = pouleService.getTournamentResults(t.getId());
            double coeff = t.isNational() ? NATIONAL_COEFFICIENT : DEFAULT_COEFFICIENT;

            for (var standing : results.standings()) {
                int basePoints = resolvePoints(standing.rank());
                int finalPoints = (int) Math.round(basePoints * coeff);

                var entry = new RankingEntryResponse.TournamentResult(
                        t.getId(),
                        t.getName(),
                        t.getDate().toString(),
                        t.isNational(),
                        standing.rank(),
                        basePoints,
                        coeff,
                        finalPoints,
                        false // will be set later
                );

                athleteResults
                        .computeIfAbsent(standing.athleteId(), id -> new ArrayList<>())
                        .add(entry);
                athleteNames.put(standing.athleteId(), standing.fullName());
                athleteClubs.put(standing.athleteId(), standing.club() != null ? standing.club() : "");
            }
        }

        // 3. For each athlete: discard the worst result, mark it, sum the best TOURNAMENTS_COUNTED
        List<RankingEntryResponse> ranking = new ArrayList<>();

        for (Map.Entry<Long, List<RankingEntryResponse.TournamentResult>> entry : athleteResults.entrySet()) {
            Long athleteId = entry.getKey();
            List<RankingEntryResponse.TournamentResult> results = entry.getValue();

            // Sort by finalPoints descending to identify the worst
            List<RankingEntryResponse.TournamentResult> sorted = results.stream()
                    .sorted(Comparator.comparingInt(RankingEntryResponse.TournamentResult::finalPoints).reversed())
                    .collect(Collectors.toList());

            // Mark the last one as discarded (only if athlete has more results than TOURNAMENTS_COUNTED)
            List<RankingEntryResponse.TournamentResult> marked = new ArrayList<>();
            int totalPoints = 0;

            for (int i = 0; i < sorted.size(); i++) {
                var r = sorted.get(i);
                boolean discarded = i >= TOURNAMENTS_COUNTED;
                marked.add(new RankingEntryResponse.TournamentResult(
                        r.tournamentId(), r.tournamentName(), r.date(), r.isNational(),
                        r.placement(), r.basePoints(), r.coefficient(), r.finalPoints(),
                        discarded
                ));
                if (!discarded) totalPoints += r.finalPoints();
            }

            ranking.add(new RankingEntryResponse(
                    0, // position assigned after sorting
                    athleteId,
                    athleteNames.get(athleteId),
                    athleteClubs.get(athleteId),
                    totalPoints,
                    results.size(),
                    marked
            ));
        }

        // 4. Sort by totalPoints descending and assign positions
        ranking.sort(Comparator.comparingInt(RankingEntryResponse::totalPoints).reversed());

        List<RankingEntryResponse> positioned = new ArrayList<>();
        for (int i = 0; i < ranking.size(); i++) {
            var r = ranking.get(i);
            positioned.add(new RankingEntryResponse(
                    i + 1, r.athleteId(), r.fullName(), r.club(),
                    r.totalPoints(), r.tournamentsPlayed(), r.tournaments()
            ));
        }

        return positioned;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Resolves the base points for a given placement using the official table.
     * Uses floorKey to match range-based entries (e.g. 9th–12th all return 8).
     */
    private int resolvePoints(int placement) {
        Integer key = POINTS_TABLE.floorKey(placement);
        if (key == null) return 0;
        Integer points = POINTS_TABLE.get(key);
        return points != null ? points : 0;
    }
}
