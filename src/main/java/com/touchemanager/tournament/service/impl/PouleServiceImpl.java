package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.bout.dto.BoutEventResponse;
import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.entity.BoutFormat;
import com.touchemanager.bout.entity.BoutStatus;
import com.touchemanager.bout.entity.EliminationRound;
import com.touchemanager.bout.repository.BoutRepository;
import com.touchemanager.bout.service.BoutService;
import com.touchemanager.shared.exception.TournamentNotFoundException;
import com.touchemanager.shared.exception.UserNotFoundException;
import com.touchemanager.tournament.dto.AssignRefereeRequest;
import com.touchemanager.tournament.dto.EliminationBracketResponse;
import com.touchemanager.tournament.dto.PouleResponse;
import com.touchemanager.tournament.dto.PouleStandingEntry;
import com.touchemanager.tournament.dto.TournamentResultResponse;
import com.touchemanager.tournament.entity.*;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.PouleRepository;
import com.touchemanager.tournament.repository.RefereeApplicationRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.service.PouleService;
import com.touchemanager.notification.service.NotificationService;
import com.touchemanager.notification.entity.NotificationType;
import com.touchemanager.tournament.sse.TournamentSseRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PouleServiceImpl implements PouleService {

    private final TournamentRepository tournamentRepository;
    private final PouleRepository pouleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final BoutRepository boutRepository;
    private final UserRepository userRepository;
    private final AthleteRepository athleteRepository;
    private final RefereeApplicationRepository refereeApplicationRepository;
    private final BoutService boutService;
    private final NotificationService notificationService;
    private final TournamentSseRegistry tournamentSseRegistry;

    // ─────────────────────────────────────────────────────────────────────────
    // Poule Generation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<PouleResponse> generatePoules(String organizerEmail, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (tournament.getPhase() != TournamentPhase.ENROLLMENT) {
            throw new IllegalStateException("Solo se pueden generar poules si el torneo esta en fase de inscripcion.");
        }
        if (pouleRepository.existsByTournamentId(tournamentId)) {
            throw new IllegalStateException("Las poules ya fueron generadas para este torneo.");
        }

        // Collect paid athletes
        List<Athlete> paidAthletes = enrollmentRepository.findByTournamentId(tournamentId)
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.PAID)
                .map(Enrollment::getAthlete)
                .collect(Collectors.toList());

        if (paidAthletes.size() < 4) {
            throw new IllegalStateException("Se necesitan al menos 4 atletas inscriptos para generar las poules.");
        }

        // Shuffle athletes for random assignment
        Collections.shuffle(paidAthletes);

        // Distribution algorithm: numPoules = ceil(N / 7)
        int n = paidAthletes.size();
        int numPoules = (int) Math.ceil(n / 7.0);
        int baseSize = n / numPoules;
        int extras = n % numPoules; // first 'extras' poules get baseSize+1 athletes

        List<Poule> poules = new ArrayList<>();
        int athleteIndex = 0;
        for (int i = 0; i < numPoules; i++) {
            int pouleSize = baseSize + (i < extras ? 1 : 0);
            List<Athlete> pouleAthletes = paidAthletes.subList(athleteIndex, athleteIndex + pouleSize);
            athleteIndex += pouleSize;

            Poule poule = new Poule();
            poule.setTournament(tournament);
            poule.setNumber(i + 1);
            poule.setStatus(PouleStatus.PENDING);
            poule.setAthletes(new ArrayList<>(pouleAthletes));
            poules.add(pouleRepository.save(poule));
        }

        // Generate round-robin bouts for each poule
        for (Poule poule : poules) {
            generateRoundRobinBouts(poule, tournament);
        }

        // Transition tournament phase
        tournament.setPhase(TournamentPhase.POULES_IN_PROGRESS);
        tournamentRepository.save(tournament);
        // Broadcast to organizer SSE subscribers
        tournamentSseRegistry.broadcast(tournament.getId());

        return poules.stream().map(this::toPouleResponse).collect(Collectors.toList());
    }

    /**
     * Generate bouts for a poule using the official FIE bout-ordering tables.
     * The tables guarantee:
     *  - Club/Nation origin protection (same-club fencers fight early)
     *  - Equitable rest (no fencer has 3 consecutive bouts)
     *  - Time optimization (next pair is always known in advance)
     *
     * Indices are 1-based fencer positions on the poule sheet.
     */
    private void generateRoundRobinBouts(Poule poule, Tournament tournament) {
        List<Athlete> athletes = poule.getAthletes();
        int n = athletes.size();

        int[][] pairings = getFieBoutOrder(n);
        validatePairings(pairings, n);

        int order = 1;
        for (int[] pair : pairings) {
            int leftIdx  = pair[0] - 1; // Convert 1-based to 0-based
            int rightIdx = pair[1] - 1;
            Bout bout = new Bout();
            bout.setTournament(tournament);
            bout.setPoule(poule);
            bout.setAthleteLeft(athletes.get(leftIdx));
            bout.setAthleteRight(athletes.get(rightIdx));
            bout.setFormat(BoutFormat.POULE);
            bout.setStatus(BoutStatus.PENDING);
            bout.setBoutOrder(order++);
            boutRepository.save(bout);
        }
    }

    /**
     * Official FIE bout-ordering tables.
     * Each int[] is {left, right} using 1-based fencer positions.
     * Source: FIE Rules for Competitions, Annex — Tables of poule bouts.
     */
    static int[][] getFieBoutOrder(int pouleSize) {
        return switch (pouleSize) {
            case 4 -> new int[][] {
                {1,2}, {3,4}, {1,3}, {2,4}, {4,1}, {2,3}
            };
            case 5 -> new int[][] {
                {1,2}, {3,4}, {5,1}, {2,3}, {5,4},
                {1,3}, {2,5}, {4,1}, {3,5}, {4,2}
            };
            case 6 -> new int[][] {
                {1,2}, {4,5}, {2,3}, {5,6}, {3,1},
                {6,4}, {2,5}, {1,4}, {5,3}, {1,6},
                {4,2}, {3,6}, {5,1}, {3,4}, {6,2}
            };
            case 7 -> new int[][] {
                {1,4}, {2,5}, {3,6}, {7,1}, {5,4}, {2,3}, {6,7},
                {5,1}, {4,3}, {6,2}, {5,7}, {3,1}, {4,6}, {7,2},
                {3,5}, {1,6}, {2,4}, {7,3}, {6,5}, {1,2}, {4,7}
            };
            default ->
                // Fallback for unexpected sizes: generic round-robin (should not happen
                // since poule generation always produces sizes 4-7)
                generateGenericPairings(pouleSize);
        };
    }

    /**
     * Guards against a malformed bout-ordering table: a poule of N fencers must
     * produce exactly C(N,2) bouts covering each unordered pair exactly once.
     * Fails loudly instead of silently generating duplicate/missing bouts.
     */
    private static void validatePairings(int[][] pairings, int n) {
        int expected = n * (n - 1) / 2;
        Set<String> seen = new HashSet<>();
        for (int[] pair : pairings) {
            int lo = Math.min(pair[0], pair[1]);
            int hi = Math.max(pair[0], pair[1]);
            if (!seen.add(lo + "-" + hi)) {
                throw new IllegalStateException(
                        "Bout-ordering table for poule size " + n + " repeats pair " + lo + "-" + hi);
            }
        }
        if (seen.size() != expected) {
            throw new IllegalStateException(
                    "Bout-ordering table for poule size " + n + " has " + seen.size()
                            + " unique pairs, expected " + expected);
        }
    }

    /** Fallback: generic N*(N-1)/2 round-robin without FIE ordering */
    private static int[][] generateGenericPairings(int n) {
        int total = n * (n - 1) / 2;
        int[][] pairs = new int[total][2];
        int idx = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = i + 1; j <= n; j++) {
                pairs[idx++] = new int[]{i, j};
            }
        }
        return pairs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Poule Queries
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PouleResponse> getPoulesForTournament(Long tournamentId) {
        return pouleRepository.findByTournamentIdOrderByNumberAsc(tournamentId)
                .stream().map(this::toPouleResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PouleResponse getPouleDetails(String refereeEmail, Long pouleId) {
        Poule poule = pouleRepository.findById(pouleId)
                .orElseThrow(() -> new IllegalArgumentException("Poule no encontrada: " + pouleId));
        // Referees must have an accepted application for the tournament
        if (refereeEmail != null) {
            assertRefereeAccepted(refereeEmail, poule.getTournament().getId());
        }
        return toPouleResponse(poule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PouleResponse> getRefereePoules(String refereeEmail, Long tournamentId) {
        User referee = getUserByEmail(refereeEmail);
        // Return only poules the referee is explicitly assigned to.
        // No acceptance check here: if not yet accepted they simply see an empty list.
        return pouleRepository.findByRefereeIdAndTournamentId(referee.getId(), tournamentId)
                .stream().map(this::toPouleResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Referee Assignment
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PouleResponse assignRefereeToPoule(String organizerEmail, Long pouleId, AssignRefereeRequest request) {
        Poule poule = pouleRepository.findById(pouleId)
                .orElseThrow(() -> new IllegalArgumentException("Poule no encontrada: " + pouleId));
        User referee = userRepository.findById(request.refereeUserId())
                .orElseThrow(() -> new UserNotFoundException(request.refereeUserId().toString()));
        if (!poule.getReferees().contains(referee)) {
            poule.getReferees().add(referee);
            pouleRepository.save(poule);

            // Notify referee of assignment
            notificationService.sendNotification(
                    referee.getId(),
                    poule.getTournament().getId(),
                    null,
                    NotificationType.REFEREE_ASSIGNMENT,
                    String.format("Fuiste asignado/a como árbitro de la Poule %d en el torneo '%s'.",
                            poule.getNumber(), poule.getTournament().getName())
            );
        }
        return toPouleResponse(poule);
    }

    @Override
    @Transactional
    public PouleResponse removeRefereeFromPoule(String organizerEmail, Long pouleId, Long refereeUserId) {
        Poule poule = pouleRepository.findById(pouleId)
                .orElseThrow(() -> new IllegalArgumentException("Poule no encontrada: " + pouleId));

        // Find the referee before removing to send notification
        poule.getReferees().stream()
                .filter(r -> r.getId().equals(refereeUserId))
                .findFirst()
                .ifPresent(referee -> notificationService.sendNotification(
                        referee.getId(),
                        poule.getTournament().getId(),
                        null,
                        NotificationType.REFEREE_ASSIGNMENT,
                        String.format("Fuiste removido/a de la Poule %d en el torneo '%s'.",
                                poule.getNumber(), poule.getTournament().getName())
                ));

        poule.getReferees().removeIf(r -> r.getId().equals(refereeUserId));
        pouleRepository.save(poule);
        return toPouleResponse(poule);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Standings Computation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PouleStandingEntry> computePouleStandings(Long tournamentId) {
        List<Poule> poules = pouleRepository.findByTournamentIdOrderByNumberAsc(tournamentId);
        Map<Long, PouleStandingData> statsMap = new LinkedHashMap<>();
        // (athleteA → athleteB → winnerId) for the FIE head-to-head tie-break
        Map<Long, Map<Long, Long>> headToHead = new HashMap<>();

        for (Poule poule : poules) {
            // Initialize all athletes in this poule
            for (Athlete a : poule.getAthletes()) {
                statsMap.putIfAbsent(a.getId(), new PouleStandingData(a, poule.getNumber()));
            }
            // Accumulate from finished bouts
            List<Bout> bouts = boutRepository.findByPouleIdOrderByBoutOrderAsc(poule.getId());
            for (Bout bout : bouts) {
                if (bout.getStatus() != BoutStatus.FINISHED) continue;

                Long leftId = bout.getAthleteLeft().getId();
                Long rightId = bout.getAthleteRight() != null ? bout.getAthleteRight().getId() : null;

                statsMap.get(leftId).addScored(bout.getScoreLeft());
                statsMap.get(leftId).addReceived(bout.getScoreRight());

                if (rightId != null && statsMap.containsKey(rightId)) {
                    statsMap.get(rightId).addScored(bout.getScoreRight());
                    statsMap.get(rightId).addReceived(bout.getScoreLeft());
                }

                // Victories
                if (bout.getWinner() != null) {
                    Long winnerId = bout.getWinner().getId();
                    if (statsMap.containsKey(winnerId)) {
                        statsMap.get(winnerId).addVictory();
                    }
                    if (rightId != null) {
                        headToHead.computeIfAbsent(leftId, k -> new HashMap<>()).put(rightId, winnerId);
                        headToHead.computeIfAbsent(rightId, k -> new HashMap<>()).put(leftId, winnerId);
                    }
                }
            }
        }

        return statsMap.values().stream()
                .map(PouleStandingData::toEntry)
                .sorted(pouleStandingsComparator(headToHead))
                .collect(Collectors.toList());
    }

    /**
     * Official FIE tie-break order:
     * 1) victories DESC, 2) indicator (TA-TR) DESC, 3) touches scored DESC,
     * 4) on a perfect tie, direct confrontation (head-to-head winner first).
     * Each criterion is reversed independently to avoid the Java Comparator
     * pitfall where chained .reversed() inverts the whole comparator.
     */
    static Comparator<PouleStandingEntry> pouleStandingsComparator(Map<Long, Map<Long, Long>> headToHead) {
        return Comparator.<PouleStandingEntry>comparingInt(PouleStandingEntry::victories).reversed()
                .thenComparing(Comparator.comparingInt(PouleStandingEntry::indicator).reversed())
                .thenComparing(Comparator.comparingInt(PouleStandingEntry::touchesScored).reversed())
                .thenComparing((a, b) -> {
                    Long winnerId = headToHead
                            .getOrDefault(a.athleteId(), Collections.emptyMap())
                            .get(b.athleteId());
                    if (winnerId == null) return 0;          // never faced each other
                    if (winnerId.equals(a.athleteId())) return -1;
                    if (winnerId.equals(b.athleteId())) return 1;
                    return 0;
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Elimination Bracket
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EliminationBracketResponse generateEliminationBracket(String organizerEmail, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (tournament.getPhase() != TournamentPhase.POULES_IN_PROGRESS) {
            throw new IllegalStateException("Solo se puede generar el bracket si el torneo esta en fase de poules.");
        }

        List<PouleStandingEntry> standings = computePouleStandings(tournamentId);

        // Determine how many advance
        int total = standings.size();
        BigDecimal rate = tournament.getAdvancementRate();
        int advancing = rate.multiply(BigDecimal.valueOf(total)).setScale(0, RoundingMode.CEILING).intValue();
        advancing = Math.max(advancing, 2);
        advancing = Math.min(advancing, total);

        List<PouleStandingEntry> qualified = standings.subList(0, advancing);

        // Tableau size = next power of 2 >= advancing
        int tableauSize = 1;
        while (tableauSize < advancing) tableauSize <<= 1;

        // Determine round
        EliminationRound firstRound = roundForTableau(tableauSize);

        // Build FIE-standard slot ordering for this tableau size.
        // buildFieTableau(n) returns a list of 'n' seed numbers in bracket slot order,
        // so that consecutive pairs (slots 0-1, 2-3, ...) are the first-round bouts and
        // the top two seeds can only meet in the final.
        // Example for n=8: [1, 8, 5, 4, 3, 6, 7, 2]
        List<Integer> slots = buildFieTableau(tableauSize);

        List<Bout> eliminationBouts = new ArrayList<>();
        int half = tableauSize / 2;
        for (int pos = 1; pos <= half; pos++) {
            // Each pair of consecutive slots forms one bout
            int leftSeed  = slots.get((pos - 1) * 2);
            int rightSeed = slots.get((pos - 1) * 2 + 1);

            Athlete left  = getAthleteForSeed(qualified, leftSeed);
            Athlete right = getAthleteForSeed(qualified, rightSeed); // null = BYE

            // Normalise: if left is the BYE slot and right is a real athlete, swap so that
            // athleteLeft always holds the real competitor and athleteRight == null marks the BYE.
            if (left == null && right != null) {
                left  = right;
                right = null;
            }

            Bout bout = new Bout();
            bout.setTournament(tournament);
            bout.setFormat(BoutFormat.ELIMINATION);
            bout.setEliminationRound(firstRound);
            bout.setBracketPosition(pos);
            bout.setAthleteLeft(left);
            bout.setAthleteRight(right);

            if (right == null) {
                // BYE: auto-finish, left advances
                bout.setStatus(BoutStatus.FINISHED);
                bout.setWinner(left);
                bout.setFinishedAt(LocalDateTime.now());
            } else {
                bout.setStatus(BoutStatus.PENDING);
            }
            eliminationBouts.add(boutRepository.save(bout));
        }

        tournament.setPhase(TournamentPhase.ELIMINATION_IN_PROGRESS);
        tournamentRepository.save(tournament);

        // Process BYE bouts: their winners must feed into the next round
        List<Bout> byeBouts = eliminationBouts.stream()
                .filter(b -> b.getAthleteRight() == null && b.getStatus() == BoutStatus.FINISHED)
                .toList();
        for (Bout byeBout : byeBouts) {
            boutService.advanceEliminationWinner(byeBout);
        }

        // Re-fetch all bouts to include any newly created next-round bouts
        List<Bout> allBouts = boutRepository
                .findByTournamentIdAndPouleIsNullOrderByEliminationRoundAscBracketPositionAsc(tournamentId);
        tournamentSseRegistry.broadcast(tournamentId);
        return buildBracketResponse(tournament, allBouts);
    }

    @Override
    @Transactional(readOnly = true)
    public EliminationBracketResponse getEliminationBracket(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        List<Bout> bouts = boutRepository
                .findByTournamentIdAndPouleIsNullOrderByEliminationRoundAscBracketPositionAsc(tournamentId);
        return buildBracketResponse(tournament, bouts);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tournament Results
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TournamentResultResponse getTournamentResults(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        // Build podium from elimination bouts
        List<TournamentResultResponse.PodiumEntry> podium = new ArrayList<>();
        List<Bout> elimBouts = boutRepository
                .findByTournamentIdAndPouleIsNullOrderByEliminationRoundAscBracketPositionAsc(tournamentId);

        // Find the FINAL bout
        Bout finalBout = elimBouts.stream()
                .filter(b -> b.getEliminationRound() == EliminationRound.FINAL)
                .findFirst().orElse(null);

        if (finalBout != null && finalBout.getStatus() == BoutStatus.FINISHED && finalBout.getWinner() != null) {
            // 1st place = winner of final
            Athlete first = finalBout.getWinner();
            podium.add(new TournamentResultResponse.PodiumEntry(1, first.getId(),
                    first.getFirstName() + " " + first.getLastName(), first.getClub()));

            // 2nd place = loser of final
            Athlete second = finalBout.getWinner().getId().equals(finalBout.getAthleteLeft().getId())
                    ? finalBout.getAthleteRight()
                    : finalBout.getAthleteLeft();
            if (second != null) {
                podium.add(new TournamentResultResponse.PodiumEntry(2, second.getId(),
                        second.getFirstName() + " " + second.getLastName(), second.getClub()));
            }

            // 3rd place = both semifinal losers
            List<Bout> semiFinals = elimBouts.stream()
                    .filter(b -> b.getEliminationRound() == EliminationRound.SEMIFINAL
                            && b.getStatus() == BoutStatus.FINISHED
                            && b.getWinner() != null)
                    .toList();
            for (Bout semi : semiFinals) {
                Athlete loser = semi.getWinner().getId().equals(semi.getAthleteLeft().getId())
                        ? semi.getAthleteRight()
                        : semi.getAthleteLeft();
                if (loser != null) {
                    podium.add(new TournamentResultResponse.PodiumEntry(3, loser.getId(),
                            loser.getFirstName() + " " + loser.getLastName(), loser.getClub()));
                }
            }
        }

        // Get initial poule standings to serve as seeding / tie-breaker for elimination losers
        List<PouleStandingEntry> pouleStandings = computePouleStandings(tournamentId);
        Map<Long, Integer> pouleRankMap = new HashMap<>();
        for (int i = 0; i < pouleStandings.size(); i++) {
            pouleRankMap.put(pouleStandings.get(i).athleteId(), i);
        }

        // Determine elimination round for each athlete
        Map<Long, Integer> athleteTiers = new HashMap<>();
        for (PouleStandingEntry entry : pouleStandings) {
            athleteTiers.put(entry.athleteId(), 8); // Default to Tier 8 (unqualified / only poules)
        }

        for (Bout bout : elimBouts) {
            if (bout.getStatus() != BoutStatus.FINISHED) continue;
            if (bout.getWinner() == null) continue;

            Athlete winner = bout.getWinner();
            Athlete loser = winner.getId().equals(bout.getAthleteLeft().getId())
                    ? bout.getAthleteRight()
                    : bout.getAthleteLeft();

            if (loser != null) {
                athleteTiers.put(loser.getId(), getTierForRound(bout.getEliminationRound()));
            }

            if (bout.getEliminationRound() == EliminationRound.FINAL) {
                athleteTiers.put(winner.getId(), 1); // Champion is Tier 1
            }
        }

        // Initialize StandingData for all athletes in the tournament (from poules)
        Map<Long, StandingData> statsMap = new LinkedHashMap<>();
        List<Poule> poules = pouleRepository.findByTournamentIdOrderByNumberAsc(tournamentId);
        for (Poule poule : poules) {
            for (Athlete a : poule.getAthletes()) {
                statsMap.putIfAbsent(a.getId(), new StandingData(a));
            }
        }

        // Build full standings stats from all finished bouts
        List<Bout> finishedBouts = boutRepository.findByTournamentIdAndStatus(tournamentId, BoutStatus.FINISHED);
        for (Bout bout : finishedBouts) {
            if (bout.getAthleteRight() == null) continue; // Skip BYE bouts

            long leftId = bout.getAthleteLeft().getId();
            long rightId = bout.getAthleteRight().getId();

            StandingData leftStats = statsMap.get(leftId);
            StandingData rightStats = statsMap.get(rightId);

            if (leftStats != null && rightStats != null) {
                leftStats.bouts++;
                rightStats.bouts++;
                leftStats.touchesScored += bout.getScoreLeft();
                leftStats.touchesReceived += bout.getScoreRight();
                rightStats.touchesScored += bout.getScoreRight();
                rightStats.touchesReceived += bout.getScoreLeft();

                if (bout.getWinner() != null) {
                    if (bout.getWinner().getId().equals(leftId)) {
                        leftStats.victories++;
                        rightStats.defeats++;
                    } else {
                        rightStats.victories++;
                        leftStats.defeats++;
                    }
                }
            }
        }

        // Sort by elimination tier first, then by initial poules rank
        List<StandingData> sorted = new ArrayList<>(statsMap.values());
        sorted.sort((a, b) -> {
            int tierA = athleteTiers.getOrDefault(a.athlete.getId(), 8);
            int tierB = athleteTiers.getOrDefault(b.athlete.getId(), 8);
            if (tierA != tierB) {
                return Integer.compare(tierA, tierB);
            }
            int pRankA = pouleRankMap.getOrDefault(a.athlete.getId(), Integer.MAX_VALUE);
            int pRankB = pouleRankMap.getOrDefault(b.athlete.getId(), Integer.MAX_VALUE);
            return Integer.compare(pRankA, pRankB);
        });

        // ── FinalStanding: final position with combined stats ────────────────
        List<TournamentResultResponse.FinalStanding> standings = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            StandingData s = sorted.get(i);
            int tier = athleteTiers.getOrDefault(s.athlete.getId(), 8);
            int rank = (tier == 3) ? 3 : (i + 1);
            standings.add(new TournamentResultResponse.FinalStanding(
                    rank, s.athlete.getId(),
                    s.athlete.getFirstName() + " " + s.athlete.getLastName(),
                    s.athlete.getClub(),
                    s.bouts, s.victories, s.defeats,
                    s.touchesScored, s.touchesReceived,
                    s.touchesScored - s.touchesReceived));
        }

        // ── Participants list (ordered by poule standings = series number) ────
        List<TournamentResultResponse.Participant> participants = new ArrayList<>();
        for (int i = 0; i < pouleStandings.size(); i++) {
            PouleStandingEntry s = pouleStandings.get(i);
            StandingData sd = statsMap.get(s.athleteId());
            if (sd != null) {
                participants.add(new TournamentResultResponse.Participant(
                        i + 1,
                        s.athleteId(),
                        sd.athlete.getFirstName() + " " + sd.athlete.getLastName(),
                        sd.athlete.getClub()
                ));
            }
        }

        // ── Poule classification (stats from poule bouts only, ordered by poule rank) ──
        List<TournamentResultResponse.PouleClassificationEntry> pouleClassification = new ArrayList<>();
        for (int i = 0; i < pouleStandings.size(); i++) {
            PouleStandingEntry s = pouleStandings.get(i);
            pouleClassification.add(new TournamentResultResponse.PouleClassificationEntry(
                    i + 1,
                    s.athleteId(),
                    s.fullName(),
                    s.club(),
                    s.victories(),
                    s.touchesScored(),
                    s.touchesReceived(),
                    s.touchesScored() - s.touchesReceived()
            ));
        }

        // ── Poule sheets (cross-table) ────────────────────────────────────────
        List<TournamentResultResponse.PouleSheet> pouleSheets = buildPouleSheets(tournamentId, poules);

        // ── Elimination bracket ───────────────────────────────────────────────
        TournamentResultResponse.BracketData bracket = buildBracket(elimBouts);

        return new TournamentResultResponse(
                tournament.getId(), tournament.getName(),
                tournament.getWeapon().name(), tournament.getCategory().name(),
                tournament.getGender().name(), tournament.getLocation(),
                tournament.getDate().toString(), tournament.getPhase().name(),
                tournament.isNational(),
                participants, podium, standings, pouleClassification,
                pouleSheets, bracket);
    }

    private int getTierForRound(EliminationRound round) {
        if (round == null) return 8;
        return switch (round) {
            case FINAL -> 2;
            case SEMIFINAL -> 3;
            case QUARTERFINAL -> 4;
            case ROUND_OF_16 -> 5;
            case ROUND_OF_32 -> 6;
            case ROUND_OF_64 -> 7;
        };
    }

    // ── Poule sheet builder ───────────────────────────────────────────────────

    private List<TournamentResultResponse.PouleSheet> buildPouleSheets(
            Long tournamentId, List<Poule> poules) {

        List<TournamentResultResponse.PouleSheet> sheets = new ArrayList<>();

        for (Poule poule : poules) {
            List<Athlete> athletes = poule.getAthletes();
            int size = athletes.size();
            // index map: athleteId → 1-based position in this poule
            Map<Long, Integer> indexMap = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                indexMap.put(athletes.get(i).getId(), i + 1);
            }

            // Fetch bouts for this poule (ordered for stable display)
            List<Bout> bouts = boutRepository.findByPouleIdOrderByBoutOrderAsc(poule.getId());

            // Build cell map: key = "rowIdx-colIdx", value = "V5" / "D3"
            Map<String, String> cellMap = new HashMap<>();
            Map<Long, Integer> victories = new HashMap<>();
            Map<Long, Integer> touchesScored = new HashMap<>();
            Map<Long, Integer> touchesReceived = new HashMap<>();

            for (Athlete a : athletes) {
                victories.put(a.getId(), 0);
                touchesScored.put(a.getId(), 0);
                touchesReceived.put(a.getId(), 0);
            }

            for (Bout b : bouts) {
                if (b.getStatus() != BoutStatus.FINISHED) continue;
                if (b.getAthleteRight() == null) continue;

                int leftIdx  = indexMap.getOrDefault(b.getAthleteLeft().getId(), 0);
                int rightIdx = indexMap.getOrDefault(b.getAthleteRight().getId(), 0);
                if (leftIdx == 0 || rightIdx == 0) continue;

                boolean leftWon = b.getWinner() != null &&
                        b.getWinner().getId().equals(b.getAthleteLeft().getId());

                // Left row, right column
                cellMap.put(leftIdx + "-" + rightIdx,
                        leftWon ? "V" + b.getScoreLeft() : "D" + b.getScoreLeft());
                // Right row, left column
                cellMap.put(rightIdx + "-" + leftIdx,
                        leftWon ? "D" + b.getScoreRight() : "V" + b.getScoreRight());

                // Accumulate stats
                touchesScored.merge(b.getAthleteLeft().getId(), b.getScoreLeft(), Integer::sum);
                touchesReceived.merge(b.getAthleteLeft().getId(), b.getScoreRight(), Integer::sum);
                touchesScored.merge(b.getAthleteRight().getId(), b.getScoreRight(), Integer::sum);
                touchesReceived.merge(b.getAthleteRight().getId(), b.getScoreLeft(), Integer::sum);
                if (leftWon) {
                    victories.merge(b.getAthleteLeft().getId(), 1, Integer::sum);
                } else if (b.getWinner() != null) {
                    victories.merge(b.getAthleteRight().getId(), 1, Integer::sum);
                }
            }

            // Sort athletes by victories desc, indicator desc for rank
            List<Athlete> ranked = new ArrayList<>(athletes);
            ranked.sort((a, b) -> {
                int v = Integer.compare(
                        victories.getOrDefault(b.getId(), 0),
                        victories.getOrDefault(a.getId(), 0));
                if (v != 0) return v;
                int indA = touchesScored.getOrDefault(a.getId(), 0) - touchesReceived.getOrDefault(a.getId(), 0);
                int indB = touchesScored.getOrDefault(b.getId(), 0) - touchesReceived.getOrDefault(b.getId(), 0);
                return Integer.compare(indB, indA);
            });
            Map<Long, Integer> rankMap = new LinkedHashMap<>();
            for (int i = 0; i < ranked.size(); i++) rankMap.put(ranked.get(i).getId(), i + 1);

            // Build rows
            List<TournamentResultResponse.PouleSheet.PouleRow> rows = new ArrayList<>();
            for (Athlete a : athletes) {
                int idx = indexMap.get(a.getId());
                Map<Integer, String> cells = new LinkedHashMap<>();
                for (Athlete opp : athletes) {
                    int oppIdx = indexMap.get(opp.getId());
                    if (oppIdx == idx) continue; // diagonal
                    String cell = cellMap.getOrDefault(idx + "-" + oppIdx, "");
                    cells.put(oppIdx, cell);
                }
                int ts = touchesScored.getOrDefault(a.getId(), 0);
                int tr = touchesReceived.getOrDefault(a.getId(), 0);
                rows.add(new TournamentResultResponse.PouleSheet.PouleRow(
                        idx,
                        a.getId(),
                        a.getFirstName() + " " + a.getLastName(),
                        a.getClub(),
                        cells,
                        victories.getOrDefault(a.getId(), 0),
                        ts, tr, ts - tr,
                        rankMap.getOrDefault(a.getId(), 0)
                ));
            }
            sheets.add(new TournamentResultResponse.PouleSheet(poule.getNumber(), rows));
        }
        return sheets;
    }

    // ── Bracket builder ───────────────────────────────────────────────────────

    private TournamentResultResponse.BracketData buildBracket(List<Bout> elimBouts) {
        // Round display order (earliest first)
        List<EliminationRound> roundOrder = List.of(
                EliminationRound.ROUND_OF_64,
                EliminationRound.ROUND_OF_32,
                EliminationRound.ROUND_OF_16,
                EliminationRound.QUARTERFINAL,
                EliminationRound.SEMIFINAL,
                EliminationRound.FINAL
        );
        Map<String, String> roundLabels = Map.of(
                "ROUND_OF_64",  "32avos de final",
                "ROUND_OF_32",  "16avos de final",
                "ROUND_OF_16",  "Octavos de final",
                "QUARTERFINAL", "Cuartos de final",
                "SEMIFINAL",    "Semifinal",
                "FINAL",        "Final"
        );

        // Group bouts by round
        Map<EliminationRound, List<Bout>> byRound = new LinkedHashMap<>();
        for (EliminationRound r : roundOrder) {
            List<Bout> roundBouts = elimBouts.stream()
                    .filter(b -> r == b.getEliminationRound())
                    .sorted(Comparator.comparingInt(b -> b.getBracketPosition() != null ? b.getBracketPosition() : 0))
                    .toList();
            if (!roundBouts.isEmpty()) byRound.put(r, roundBouts);
        }

        List<TournamentResultResponse.BracketData.BracketRound> rounds = new ArrayList<>();
        for (Map.Entry<EliminationRound, List<Bout>> entry : byRound.entrySet()) {
            String roundKey = entry.getKey().name();
            List<TournamentResultResponse.BracketData.BracketBout> boutDtos = entry.getValue().stream()
                    .map(b -> new TournamentResultResponse.BracketData.BracketBout(
                            b.getId(),
                            b.getBracketPosition() != null ? b.getBracketPosition() : 0,
                            b.getAthleteLeft() != null
                                    ? b.getAthleteLeft().getFirstName() + " " + b.getAthleteLeft().getLastName()
                                    : "?",
                            b.getAthleteRight() != null
                                    ? b.getAthleteRight().getFirstName() + " " + b.getAthleteRight().getLastName()
                                    : null,   // null = BYE slot — do NOT send "BYE" string
                            b.getScoreLeft(),
                            b.getScoreRight(),
                            b.getWinner() != null
                                    ? b.getWinner().getFirstName() + " " + b.getWinner().getLastName()
                                    : null,
                            b.getStatus() == BoutStatus.FINISHED,
                            b.getStatus().name(),
                            b.getPiste()
                    ))
                    .toList();
            rounds.add(new TournamentResultResponse.BracketData.BracketRound(
                    roundKey,
                    roundLabels.getOrDefault(roundKey, roundKey),
                    boutDtos
            ));
        }
        return new TournamentResultResponse.BracketData(rounds);
    }

    /** Helper class for standings accumulation in getTournamentResults */
    private static class StandingData {
        final Athlete athlete;
        int bouts = 0, victories = 0, defeats = 0, touchesScored = 0, touchesReceived = 0;
        StandingData(Athlete a) { this.athlete = a; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the FIE-standard bracket slot list for a tableau of the given power-of-2 size.
     *
     * The algorithm works recursively:
     *   - A tableau of size 1 has a single slot: [1]
     *   - Each step doubles the list by expanding every seed s into [s, n+1-s]
     *     where n is the current size, effectively interleaving each seed with
     *     its "complement" opponent.
     *
     * Result for size 8: [1, 8, 5, 4, 3, 6, 7, 2]
     * This guarantees that seeds can only meet opponents from the correct half
     * of the bracket, so seeds 1 and 2 can only meet in the final.
     */
    private List<Integer> buildFieTableau(int size) {
        List<Integer> slots = new ArrayList<>();
        slots.add(1);
        int currentSize = 1;
        while (currentSize < size) {
            currentSize *= 2;
            List<Integer> next = new ArrayList<>();
            for (int seed : slots) {
                next.add(seed);
                next.add(currentSize + 1 - seed);
            }
            slots = next;
        }
        return slots;
    }

    /**
     * Returns the Athlete entity for a given 1-based seed position from the standings list.
     * Returns null if the seed is out of bounds (BYE slot).
     */
    private Athlete getAthleteForSeed(List<PouleStandingEntry> qualified, int seed) {
        if (seed > qualified.size()) return null; // BYE
        Long athleteId = qualified.get(seed - 1).athleteId();
        return athleteRepository.findById(athleteId)
                .orElseThrow(() -> new IllegalStateException("Athlete not found for seed " + seed + ", id=" + athleteId));
    }

    private EliminationRound roundForTableau(int size) {
        return switch (size) {
            case 64 -> EliminationRound.ROUND_OF_64;
            case 32 -> EliminationRound.ROUND_OF_32;
            case 16 -> EliminationRound.ROUND_OF_16;
            case 8  -> EliminationRound.QUARTERFINAL;
            case 4  -> EliminationRound.SEMIFINAL;
            default -> EliminationRound.FINAL;
        };
    }

    private int roundForTableauFromRound(EliminationRound round) {
        return switch (round) {
            case ROUND_OF_64 -> 64;
            case ROUND_OF_32 -> 32;
            case ROUND_OF_16 -> 16;
            case QUARTERFINAL -> 8;
            case SEMIFINAL -> 4;
            case FINAL -> 2;
        };
    }

    private EliminationBracketResponse buildBracketResponse(Tournament tournament, List<Bout> bouts) {
        Map<EliminationRound, List<BoutResponse>> roundMap = new LinkedHashMap<>();
        for (EliminationRound round : EliminationRound.values()) {
            List<BoutResponse> roundBouts = bouts.stream()
                    .filter(b -> b.getEliminationRound() == round)
                    .sorted(Comparator.comparing(Bout::getBracketPosition,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::toBoutResponse)
                    .collect(Collectors.toList());
            if (!roundBouts.isEmpty()) roundMap.put(round, roundBouts);
        }

        int tableauSize = bouts.stream()
                .filter(b -> b.getEliminationRound() != null)
                .mapToInt(b -> roundForTableauFromRound(b.getEliminationRound()))
                .max().orElse(2);

        return new EliminationBracketResponse(tournament.getId(), tournament.getName(), tableauSize, roundMap);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    /** Throws AccessDeniedException if the referee has no ACCEPTED application for the tournament */
    private void assertRefereeAccepted(String refereeEmail, Long tournamentId) {
        User referee = getUserByEmail(refereeEmail);
        boolean accepted = refereeApplicationRepository.existsByRefereeIdAndTournamentIdAndStatus(
                referee.getId(), tournamentId, RefereeApplicationStatus.ACCEPTED);
        if (!accepted) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Tu solicitud para arbitrar en este torneo no fue aceptada aún.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Start Poule
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PouleResponse startPoule(String refereeEmail, Long pouleId) {
        Poule poule = pouleRepository.findById(pouleId)
                .orElseThrow(() -> new IllegalArgumentException("Poule no encontrada: " + pouleId));

        // Validate referee is assigned to this poule
        User referee = getUserByEmail(refereeEmail);
        boolean isAssigned = poule.getReferees().stream()
                .anyMatch(r -> r.getId().equals(referee.getId()));
        if (!isAssigned) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No estás asignado como árbitro de esta poule.");
        }

        if (poule.getStatus() != PouleStatus.PENDING) {
            throw new IllegalStateException(
                    "La poule ya fue iniciada o finalizada (estado actual: " + poule.getStatus() + ").");
        }

        // Transition poule to IN_PROGRESS
        poule.setStatus(PouleStatus.IN_PROGRESS);
        pouleRepository.save(poule);

        // Send YOUR_TURN to athletes of the first bout (boutOrder = 1)
        boutRepository.findByPouleIdAndBoutOrder(poule.getId(), 1).ifPresent(firstBout -> {
            String pouleLabel = "Poule " + poule.getNumber();
            if (firstBout.getAthleteLeft() != null && firstBout.getAthleteRight() != null) {
                String opponentLeft = firstBout.getAthleteRight().getFirstName() + " " + firstBout.getAthleteRight().getLastName();
                String opponentRight = firstBout.getAthleteLeft().getFirstName() + " " + firstBout.getAthleteLeft().getLastName();

                notificationService.sendNotification(
                        firstBout.getAthleteLeft().getUser().getId(),
                        poule.getTournament().getId(),
                        firstBout.getId(),
                        NotificationType.YOUR_TURN,
                        String.format("¡Es tu turno! Te toca disputar tu asalto contra %s en la %s.",
                                opponentLeft, pouleLabel)
                );
                notificationService.sendNotification(
                        firstBout.getAthleteRight().getUser().getId(),
                        poule.getTournament().getId(),
                        firstBout.getId(),
                        NotificationType.YOUR_TURN,
                        String.format("¡Es tu turno! Te toca disputar tu asalto contra %s en la %s.",
                                opponentRight, pouleLabel)
                );
            }
        });

        tournamentSseRegistry.broadcast(poule.getTournament().getId());
        return toPouleResponse(poule);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private PouleResponse toPouleResponse(Poule poule) {
        List<Bout> bouts = boutRepository.findByPouleIdOrderByBoutOrderAsc(poule.getId());
        long finished = bouts.stream().filter(b -> b.getStatus() == BoutStatus.FINISHED).count();

        List<PouleResponse.PouleAthleteInfo> athletes = poule.getAthletes().stream()
                .map(a -> new PouleResponse.PouleAthleteInfo(
                        a.getId(),
                        a.getFirstName() + " " + a.getLastName(),
                        a.getClub()))
                .collect(Collectors.toList());

        // User entity has no firstName/lastName — email is used as the display name
        List<PouleResponse.PouleRefereeInfo> referees = poule.getReferees().stream()
                .map(u -> new PouleResponse.PouleRefereeInfo(u.getId(), u.getEmail(), u.getEmail()))
                .collect(Collectors.toList());

        return new PouleResponse(
                poule.getId(),
                poule.getTournament().getId(),
                poule.getTournament().getName(),
                poule.getNumber(),
                poule.getStatus(),
                athletes,
                referees,
                bouts.stream().map(this::toBoutResponse).collect(Collectors.toList()),
                bouts.size(),
                (int) finished
        );
    }

    /**
     * Maps a Bout entity to BoutResponse.
     *
     * BoutResponse record signature (from bout/dto/BoutResponse.java):
     *   (Long id, Long tournamentId, String tournamentName, BoutFormat format, BoutStatus status,
     *    AthleteSummary athleteLeft, AthleteSummary athleteRight,
     *    int scoreLeft, int scoreRight,
     *    int currentPeriod, int maxPeriods, int touchesTarget,
     *    int elapsedSeconds, Long winnerId,
     *    LocalDateTime startedAt, LocalDateTime finishedAt,
     *    List<BoutEventResponse> events)
     */
    private BoutResponse toBoutResponse(Bout bout) {
        List<BoutEventResponse> events = bout.getEvents().stream()
                .map(e -> new BoutEventResponse(
                        e.getId(),
                        e.getSide(),
                        e.getEventType(),
                        e.getScoreDelta(),
                        e.getRecordedAt()))
                .collect(Collectors.toList());

        List<BoutResponse.RefereeInfo> refereeResponses = bout.getReferees().stream()
                .map(r -> new BoutResponse.RefereeInfo(
                        r.getId(),
                        r.getEmail(),
                        r.getEmail()
                ))
                .collect(Collectors.toList());

        Long pouleId = bout.getPoule() != null ? bout.getPoule().getId() : null;
        Integer pouleNumber = bout.getPoule() != null ? bout.getPoule().getNumber() : null;

        return new BoutResponse(
                bout.getId(),
                bout.getTournament().getId(),
                bout.getTournament().getName(),
                pouleId,
                pouleNumber,
                bout.getBoutOrder(),
                bout.getPiste(),
                bout.getFormat(),
                bout.getStatus(),
                toAthleteSummary(bout.getAthleteLeft()),
                bout.getAthleteRight() != null ? toAthleteSummary(bout.getAthleteRight()) : null,
                bout.getScoreLeft(),
                bout.getScoreRight(),
                bout.getCurrentPeriod(),
                maxPeriods(bout.getFormat()),
                touchesTarget(bout.getFormat()),
                bout.getElapsedSeconds(),
                bout.isTimerPaused(),
                bout.getWinner() != null ? bout.getWinner().getId() : null,
                bout.getEliminationRound(),
                bout.getBracketPosition(),
                bout.getPriority(),
                bout.getStartedAt(),
                bout.getFinishedAt(),
                events,
                refereeResponses
        );
    }

    private BoutResponse.AthleteSummary toAthleteSummary(Athlete a) {
        return new BoutResponse.AthleteSummary(
                a.getId(),
                a.getUser().getId(),
                a.getFirstName(),
                a.getLastName(),
                a.getClub()
        );
    }

    private int touchesTarget(BoutFormat format) {
        return format == BoutFormat.POULE ? 5 : 15;
    }

    private int maxPeriods(BoutFormat format) {
        return format == BoutFormat.POULE ? 1 : 3;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner class for standings accumulation
    // ─────────────────────────────────────────────────────────────────────────

    private static class PouleStandingData {
        final Athlete athlete;
        final int pouleNumber;
        int victories = 0;
        int scored = 0;
        int received = 0;

        PouleStandingData(Athlete a, int pouleNumber) {
            this.athlete = a;
            this.pouleNumber = pouleNumber;
        }

        void addVictory()  { victories++; }
        void addScored(int s)   { scored += s; }
        void addReceived(int r) { received += r; }

        PouleStandingEntry toEntry() {
            return new PouleStandingEntry(
                    athlete.getId(),
                    athlete.getFirstName() + " " + athlete.getLastName(),
                    athlete.getClub(),
                    pouleNumber,
                    victories,
                    scored,
                    received,
                    scored - received
            );
        }
    }
}
