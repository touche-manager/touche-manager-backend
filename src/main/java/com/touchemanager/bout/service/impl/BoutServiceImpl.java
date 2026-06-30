package com.touchemanager.bout.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.bout.dto.BoutEventRequest;
import com.touchemanager.bout.dto.BoutEventResponse;
import com.touchemanager.bout.dto.BoutLiveUpdate;
import com.touchemanager.bout.dto.BoutRequest;
import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.bout.dto.LiveBoutSummary;
import com.touchemanager.bout.dto.TournamentStandingsResponse;
import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.entity.BoutEvent;
import com.touchemanager.bout.entity.BoutFormat;
import com.touchemanager.bout.entity.BoutStatus;
import com.touchemanager.bout.entity.EliminationRound;
import com.touchemanager.bout.entity.EventSide;
import com.touchemanager.bout.entity.EventType;
import com.touchemanager.bout.repository.BoutEventRepository;
import com.touchemanager.bout.repository.BoutRepository;
import com.touchemanager.bout.service.BoutService;
import com.touchemanager.bout.sse.BoutSseEmitterRegistry;
import com.touchemanager.shared.exception.BoutNotFoundException;
import com.touchemanager.shared.exception.TournamentNotFoundException;
import com.touchemanager.notification.service.NotificationService;
import com.touchemanager.notification.entity.NotificationType;
import com.touchemanager.notification.dto.NotificationDTO;
import com.touchemanager.shared.exception.UserNotFoundException;
import com.touchemanager.tournament.dto.AssignRefereeRequest;
import com.touchemanager.tournament.dto.OrganizerTournamentResponse;
import com.touchemanager.tournament.entity.Enrollment;
import com.touchemanager.tournament.entity.EnrollmentStatus;
import com.touchemanager.tournament.entity.Poule;
import com.touchemanager.tournament.entity.PouleStatus;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.TournamentPhase;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.PouleRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.sse.TournamentSseRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoutServiceImpl implements BoutService {

    private static final Logger log = LoggerFactory.getLogger(BoutServiceImpl.class);

    private final BoutRepository boutRepository;
    private final BoutEventRepository boutEventRepository;
    private final TournamentRepository tournamentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AthleteRepository athleteRepository;
    private final UserRepository userRepository;
    private final PouleRepository pouleRepository;
    private final NotificationService notificationService;
    private final BoutSseEmitterRegistry sseEmitterRegistry;
    private final TournamentSseRegistry tournamentSseRegistry;

    @Override
    @Transactional(readOnly = true)
    public List<OrganizerTournamentResponse> getAllTournaments() {
        return tournamentRepository.findAllByOrderByDateAsc().stream()
                .map(t -> {
                    List<Enrollment> enrollments = enrollmentRepository.findByTournamentId(t.getId());
                    long paid    = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.PAID).count();
                    long pending = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.PENDING_PAYMENT).count();
                    long cancelled = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.CANCELLED).count();
                    return new OrganizerTournamentResponse(
                            t.getId(), t.getName(), t.getWeapon(), t.getCategory(), t.getGender(),
                            t.getLocation(), t.getDate(), t.getBasePrice(),
                            t.getPhase(), t.getAdvancementRate(),
                            t.isNational(),
                            enrollments.size(), paid, pending, cancelled);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BoutResponse createBout(String email, BoutRequest request) {
        Tournament tournament = tournamentRepository.findById(request.getTournamentId())
                .orElseThrow(() -> new TournamentNotFoundException(request.getTournamentId()));

        Athlete left = athleteRepository.findById(request.getAthleteLeftId())
                .orElseThrow(() -> new IllegalArgumentException("Athlete not found with ID: " + request.getAthleteLeftId()));

        Athlete right = athleteRepository.findById(request.getAthleteRightId())
                .orElseThrow(() -> new IllegalArgumentException("Athlete not found with ID: " + request.getAthleteRightId()));


        if (left.getId().equals(right.getId())) {
            throw new IllegalArgumentException("A fencer cannot bout against themselves");
        }

        Bout bout = new Bout();
        bout.setTournament(tournament);
        bout.setAthleteLeft(left);
        bout.setAthleteRight(right);
        bout.setFormat(request.getFormat());

        Bout saved = boutRepository.save(bout);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public BoutResponse startBout(String email, Long boutId) {
        Bout bout = getBout(boutId);

        if (bout.getStatus() != BoutStatus.PENDING) {
            throw new IllegalArgumentException("Bout has already been started or finished");
        }

        bout.setStatus(BoutStatus.IN_PROGRESS);
        bout.setStartedAt(LocalDateTime.now());

        // Tarea 1: Transition poule from PENDING → IN_PROGRESS on first bout start
        if (bout.getPoule() != null && bout.getPoule().getStatus() == PouleStatus.PENDING) {
            bout.getPoule().setStatus(PouleStatus.IN_PROGRESS);
            pouleRepository.save(bout.getPoule());
            log.info("Poule {} transitioned to IN_PROGRESS", bout.getPoule().getNumber());
        }

        Bout saved = boutRepository.save(bout);

        // Notify fencers of the NEXT bout (if this is a poule bout)
        if (saved.getPoule() != null && saved.getBoutOrder() != null) {
            Optional<Bout> nextBoutOpt = boutRepository.findByPouleIdAndBoutOrder(
                    saved.getPoule().getId(), saved.getBoutOrder() + 1);
            if (nextBoutOpt.isPresent()) {
                Bout nextBout = nextBoutOpt.get();
                String pisteName = nextBout.getPiste() != null ? nextBout.getPiste() : "pista a confirmar";

                // Notify Left fencer
                if (nextBout.getAthleteLeft() != null) {
                    String opponentName = nextBout.getAthleteRight() != null 
                        ? nextBout.getAthleteRight().getFirstName() + " " + nextBout.getAthleteRight().getLastName()
                        : "rival a confirmar";
                    notificationService.sendNotification(
                            nextBout.getAthleteLeft().getUser().getId(),
                            nextBout.getTournament().getId(),
                            nextBout.getId(),
                            NotificationType.NEXT_UP,
                            String.format("¡Preparate! Te toca el próximo asalto contra %s en la pista %s.",
                                    opponentName, pisteName)
                    );
                }

                // Notify Right fencer
                if (nextBout.getAthleteRight() != null) {
                    String opponentName = nextBout.getAthleteLeft() != null 
                        ? nextBout.getAthleteLeft().getFirstName() + " " + nextBout.getAthleteLeft().getLastName()
                        : "rival a confirmar";
                    notificationService.sendNotification(
                            nextBout.getAthleteRight().getUser().getId(),
                            nextBout.getTournament().getId(),
                            nextBout.getId(),
                            NotificationType.NEXT_UP,
                            String.format("¡Preparate! Te toca el próximo asalto contra %s en la pista %s.",
                                    opponentName, pisteName)
                    );
                }
            }
        }

        publishLiveUpdate(saved);
        // Notify tournament SSE subscribers so spectator poule tables update instantly
        if (saved.getTournament() != null) {
            tournamentSseRegistry.broadcast(saved.getTournament().getId());
        }
        return toResponse(saved);
    }

    @Override
    @Transactional
    public BoutResponse recordEvent(String email, Long boutId, BoutEventRequest request) {
        Bout bout = getBout(boutId);

        if (bout.getStatus() != BoutStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Bout is not in progress");
        }

        // ── Score limit guard ──────────────────────────────────────────────────
        if (request.getEventType() == EventType.TOUCHE) {
            // Fencer scoring: their own score must be below the limit
            int limit = touchesTarget(bout.getFormat());
            int currentScore = request.getSide() == EventSide.LEFT ? bout.getScoreLeft() : bout.getScoreRight();
            if (currentScore >= limit) {
                throw new IllegalArgumentException(
                    "Límite de toques alcanzado: no se pueden sumar más de " + limit + " toques en este formato");
            }
        } else if (request.getEventType() == EventType.RED_CARD) {
            // Red card: point goes to the OPPONENT — check the opponent's score
            int limit = touchesTarget(bout.getFormat());
            int opponentScore = request.getSide() == EventSide.LEFT ? bout.getScoreRight() : bout.getScoreLeft();
            if (opponentScore >= limit) {
                throw new IllegalArgumentException(
                    "Límite de toques alcanzado: el adversario ya tiene el máximo de toques permitidos");
            }
        }

        int scoreDelta = resolveScoreDelta(request.getEventType());

        // Create event — side always refers to the fencer who performed the action or received the card
        BoutEvent event = new BoutEvent();
        event.setBout(bout);
        event.setSide(request.getSide());
        event.setEventType(request.getEventType());
        event.setScoreDelta(scoreDelta);
        event.setRecordedAt(LocalDateTime.now());
        event.setRefereeEmail(email);
        boutEventRepository.save(event);

        // ── Determine which side's score is affected ──────────────────────────
        // RED_CARD           → +1 to the OPPONENT
        // RED_CARD_REMOVAL   → -1 from the OPPONENT (reverses the red card point)
        // TOUCHE             → +1 to the fencer on request.getSide()
        // SCORE_CORRECTION   → -1 from request.getSide() (min 0)
        // YELLOW_CARD / YELLOW_CARD_REMOVAL → no score change (delta = 0)
        EventSide scoringSide;
        if (request.getEventType() == EventType.RED_CARD
                || request.getEventType() == EventType.RED_CARD_REMOVAL) {
            // Both RED_CARD and RED_CARD_REMOVAL affect the opponent's score
            scoringSide = request.getSide() == EventSide.LEFT ? EventSide.RIGHT : EventSide.LEFT;
        } else {
            scoringSide = request.getSide();
        }

        if (scoreDelta != 0) {
            if (scoringSide == EventSide.LEFT) {
                bout.setScoreLeft(Math.max(0, bout.getScoreLeft() + scoreDelta));
            } else {
                bout.setScoreRight(Math.max(0, bout.getScoreRight() + scoreDelta));
            }
        }

        // Recording any event always pauses the clock on the referee's side.
        // Set timerPaused=true here to avoid the race condition where this SSE
        // fires before the concurrent PATCH /time call has updated the DB.
        bout.setTimerPaused(true);

        Bout saved = boutRepository.save(bout);
        publishLiveUpdate(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public BoutResponse updateElapsedTime(String email, Long boutId, int elapsedSeconds) {
        Bout bout = getBout(boutId);

        if (bout.getStatus() != BoutStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Bout is not in progress");
        }

        bout.setElapsedSeconds(elapsedSeconds);
        Bout saved = boutRepository.save(bout);
        publishLiveUpdate(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public BoutResponse updateTimerState(String email, Long boutId, int elapsedSeconds, boolean timerPaused, Integer currentPeriod) {
        Bout bout = getBout(boutId);

        if (bout.getStatus() != BoutStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Bout is not in progress");
        }

        bout.setElapsedSeconds(elapsedSeconds);
        bout.setTimerPaused(timerPaused);
        if (currentPeriod != null) {
            bout.setCurrentPeriod(currentPeriod);
        }
        Bout saved = boutRepository.save(bout);
        publishLiveUpdate(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public BoutResponse finishBout(String email, Long boutId) {
        Bout bout = getBout(boutId);

        if (bout.getStatus() == BoutStatus.FINISHED) {
            throw new IllegalArgumentException("Bout is already finished");
        }

        // Anti-tie: if scores are tied, require priority to be set
        if (bout.getScoreLeft() == bout.getScoreRight()) {
            if (bout.getPriority() == null) {
                throw new IllegalStateException(
                    "No se puede finalizar un asalto empatado sin asignar prioridad. " +
                    "Sortee la prioridad antes de finalizar.");
            }
            // Priority holder wins when scores are tied
            bout.setStatus(BoutStatus.FINISHED);
            bout.setFinishedAt(LocalDateTime.now());
            if (bout.getPriority() == EventSide.LEFT) {
                bout.setWinner(bout.getAthleteLeft());
            } else {
                bout.setWinner(bout.getAthleteRight());
            }
        } else {
            declareWinner(bout);
        }

        afterBoutFinished(bout);
        Bout saved = boutRepository.save(bout);
        publishLiveUpdate(saved);
        return toResponse(saved);
    }

    // ── Priority Assignment ─────────────────────────────────────────────────

    @Override
    @Transactional
    public BoutResponse assignPriority(String email, Long boutId, EventSide side) {
        Bout bout = getBout(boutId);

        if (bout.getStatus() == BoutStatus.FINISHED) {
            throw new IllegalArgumentException("No se puede asignar prioridad a un asalto finalizado");
        }

        bout.setPriority(side);
        bout.setTimerPaused(true);
        log.info("Priority assigned to {} in bout {}", side, boutId);
        Bout saved = boutRepository.save(bout);
        publishLiveUpdate(saved);
        return toResponse(saved);
    }

    // ── Piste Assignment ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public BoutResponse updatePiste(String email, Long boutId, String piste) {
        Bout bout = getBout(boutId);
        bout.setPiste(piste);
        log.info("Piste '{}' assigned to bout {}", piste, boutId);
        Bout saved = boutRepository.save(bout);
        publishLiveUpdate(saved);
        return toResponse(saved);
    }

    // ── Live scoreboard (SSE) ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BoutLiveUpdate getLiveSnapshot(Long boutId) {
        return toLiveUpdate(getBout(boutId));
    }

    private void publishLiveUpdate(Bout bout) {
        sseEmitterRegistry.broadcast(bout.getId(), toLiveUpdate(bout));
    }

    private BoutLiveUpdate toLiveUpdate(Bout bout) {
        return new BoutLiveUpdate(
                bout.getId(),
                bout.getScoreLeft(),
                bout.getScoreRight(),
                fullName(bout.getAthleteLeft()),
                bout.getAthleteRight() != null ? fullName(bout.getAthleteRight()) : "BYE",
                bout.getStatus(),
                bout.getElapsedSeconds(),
                bout.getCurrentPeriod(),
                bout.getPiste(),
                bout.getWinner() != null ? fullName(bout.getWinner()) : null,
                bout.getStatus() == BoutStatus.IN_PROGRESS && !bout.isTimerPaused()
        );
    }

    private String fullName(Athlete athlete) {
        return athlete.getFirstName() + " " + athlete.getLastName();
    }

    // ── Post-finish hooks (poule status + bracket advancement) ───────────────

    private void afterBoutFinished(Bout bout) {
        // Tarea 1: Check if all bouts in the poule are finished → FINISHED
        if (bout.getPoule() != null) {
            Poule poule = bout.getPoule();
            List<Bout> pouleBouts = boutRepository.findByPouleIdOrderByBoutOrderAsc(poule.getId());
            boolean allFinished = pouleBouts.stream().allMatch(b -> b.getStatus() == BoutStatus.FINISHED);
            if (allFinished) {
                poule.setStatus(PouleStatus.FINISHED);
                pouleRepository.save(poule);
                log.info("Poule {} transitioned to FINISHED", poule.getNumber());
            }
        }

        // Tarea 2: Advance elimination winner to next round
        if (bout.getFormat() == BoutFormat.ELIMINATION && bout.getEliminationRound() != null) {
            advanceEliminationWinner(bout);
        }

        // Notify organizer SSE subscribers that tournament data changed
        if (bout.getTournament() != null) {
            tournamentSseRegistry.broadcast(bout.getTournament().getId());
        }
    }

    /**
     * When an elimination bout finishes, check if its partner bout is also finished.
     * If so, create the next-round bout with both winners.
     * If this was the FINAL, transition the tournament to FINISHED.
     */
    @Override
    @Transactional
    public void advanceEliminationWinner(Bout finishedBout) {
        if (finishedBout.getWinner() == null) {
            log.warn("Elimination bout {} finished without a winner — cannot advance", finishedBout.getId());
            return;
        }

        EliminationRound currentRound = finishedBout.getEliminationRound();

        // If FINAL, the tournament is finished
        if (currentRound == EliminationRound.FINAL) {
            Tournament tournament = finishedBout.getTournament();
            tournament.setPhase(TournamentPhase.FINISHED);
            tournamentRepository.save(tournament);
            log.info("Tournament '{}' transitioned to FINISHED. Winner: {} {}",
                    tournament.getName(),
                    finishedBout.getWinner().getFirstName(),
                    finishedBout.getWinner().getLastName());
            return;
        }

        int myPos = finishedBout.getBracketPosition();
        // Positions (2k-1) and (2k) feed into position k of next round
        int partnerPos = (myPos % 2 == 1) ? myPos + 1 : myPos - 1;
        EliminationRound nextRound = currentRound.getNext();
        int nextPos = (int) Math.ceil(myPos / 2.0);

        // Find partner bout in same round
        Optional<Bout> partnerOpt = boutRepository
                .findByTournamentIdAndEliminationRoundAndBracketPosition(
                        finishedBout.getTournament().getId(), currentRound, partnerPos);

        if (partnerOpt.isEmpty()) {
            // Partner doesn't exist (odd number of bouts in round — shouldn't happen with power-of-2 tableau)
            log.warn("No partner bout found for position {} in round {} — creating solo advancement",
                    myPos, currentRound);
            return;
        }

        Bout partner = partnerOpt.get();
        if (partner.getStatus() != BoutStatus.FINISHED) {
            // Partner hasn't finished yet — the next-round bout will be created when it finishes
            log.info("Waiting for partner bout {} to finish before creating next-round bout", partner.getId());
            return;
        }

        // Both finished — determine left/right based on bracket position parity
        // Odd position winner goes LEFT, even position winner goes RIGHT
        Athlete left;
        Athlete right;
        if (myPos % 2 == 1) {
            // I am the odd position
            left = finishedBout.getWinner();
            right = partner.getWinner();
        } else {
            // I am the even position
            left = partner.getWinner();
            right = finishedBout.getWinner();
        }

        // Check if next-round bout already exists (created by the partner finishing first)
        Optional<Bout> existingNext = boutRepository
                .findByTournamentIdAndEliminationRoundAndBracketPosition(
                        finishedBout.getTournament().getId(), nextRound, nextPos);
        if (existingNext.isPresent()) {
            log.info("Next-round bout already exists at round {} pos {} — skipping", nextRound, nextPos);
            return;
        }

        Bout nextBout = new Bout();
        nextBout.setTournament(finishedBout.getTournament());
        nextBout.setAthleteLeft(left);
        nextBout.setAthleteRight(right);
        nextBout.setFormat(BoutFormat.ELIMINATION);
        nextBout.setStatus(BoutStatus.PENDING);
        nextBout.setEliminationRound(nextRound);
        nextBout.setBracketPosition(nextPos);
        boutRepository.save(nextBout);

        log.info("Created next-round bout: {} vs {} in round {} position {}",
                left.getFirstName() + " " + left.getLastName(),
                right.getFirstName() + " " + right.getLastName(),
                nextRound, nextPos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoutResponse> getBoutsByTournament(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException(tournamentId);
        }
        return boutRepository.findByTournamentIdOrderByIdAsc(tournamentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BoutResponse getBoutDetails(Long boutId) {
        return toResponse(getBout(boutId));
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentStandingsResponse getTournamentStandings(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        List<Bout> finishedBouts = boutRepository.findByTournamentIdAndStatus(tournamentId, BoutStatus.FINISHED);

        // Build stats per athlete
        Map<Long, AthleteStats> statsMap = new HashMap<>();

        for (Bout bout : finishedBouts) {
            // Skip BYE bouts (no right athlete)
            if (bout.getAthleteRight() == null) continue;

            long leftId = bout.getAthleteLeft().getId();
            long rightId = bout.getAthleteRight().getId();

            AthleteStats leftStats = statsMap.computeIfAbsent(leftId, id -> new AthleteStats(bout.getAthleteLeft()));
            AthleteStats rightStats = statsMap.computeIfAbsent(rightId, id -> new AthleteStats(bout.getAthleteRight()));

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

        // Sort: 1) victories DESC, 2) indicator DESC, 3) touchesScored DESC
        List<AthleteStats> sorted = new ArrayList<>(statsMap.values());
        sorted.sort(Comparator
                .comparingInt((AthleteStats s) -> s.victories).reversed()
                .thenComparingInt(s -> -(s.touchesScored - s.touchesReceived))
                .thenComparingInt(s -> -s.touchesScored));

        List<TournamentStandingsResponse.AthleteStanding> standings = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            AthleteStats s = sorted.get(i);
            standings.add(new TournamentStandingsResponse.AthleteStanding(
                    i + 1,
                    s.athlete.getId(),
                    s.athlete.getFirstName(),
                    s.athlete.getLastName(),
                    s.athlete.getClub(),
                    s.bouts,
                    s.victories,
                    s.defeats,
                    s.touchesScored,
                    s.touchesReceived,
                    s.touchesScored - s.touchesReceived
            ));
        }

        return new TournamentStandingsResponse(tournamentId, tournament.getName(), standings);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Bout getBout(Long boutId) {
        return boutRepository.findById(boutId)
                .orElseThrow(() -> new BoutNotFoundException(boutId));
    }

    private void declareWinner(Bout bout) {
        bout.setStatus(BoutStatus.FINISHED);
        bout.setFinishedAt(LocalDateTime.now());

        if (bout.getScoreLeft() > bout.getScoreRight()) {
            bout.setWinner(bout.getAthleteLeft());
        } else if (bout.getScoreRight() > bout.getScoreLeft()) {
            bout.setWinner(bout.getAthleteRight());
        }
        // If tied, winner is resolved by priority in finishBout() — this path only
        // triggers for auto-finish on target reached (where scores differ).
    }

    private int resolveScoreDelta(EventType eventType) {
        return switch (eventType) {
            case TOUCHE, RED_CARD -> 1;           // RED_CARD gives +1 to the opponent (side inversion handled above)
            case SCORE_CORRECTION -> -1;          // remove a point from that fencer (min 0 enforced above)
            case RED_CARD_REMOVAL -> -1;          // remove the +1 the opponent got from the red card
            case YELLOW_CARD, YELLOW_CARD_REMOVAL -> 0; // warnings — no score change
        };
    }

    private int touchesTarget(BoutFormat format) {
        return format == BoutFormat.POULE ? 5 : 15;
    }

    private int maxPeriods(BoutFormat format) {
        return format == BoutFormat.POULE ? 1 : 3;
    }

    private BoutResponse toResponse(Bout bout) {
        List<BoutEventResponse> eventResponses = bout.getEvents().stream()
                .map(e -> new BoutEventResponse(
                        e.getId(),
                        e.getSide(),
                        e.getEventType(),
                        e.getScoreDelta(),
                        e.getRecordedAt()
                ))
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
                eventResponses,
                refereeResponses
        );
    }

    private BoutResponse.AthleteSummary toAthleteSummary(Athlete athlete) {
        if (athlete == null) return null;
        return new BoutResponse.AthleteSummary(
                athlete.getId(),
                athlete.getUser().getId(),
                athlete.getFirstName(),
                athlete.getLastName(),
                athlete.getClub()
        );
    }

    // ── Inner helper class for standings calculation ──────────────────────────

    private static class AthleteStats {
        final Athlete athlete;
        int bouts = 0;
        int victories = 0;
        int defeats = 0;
        int touchesScored = 0;
        int touchesReceived = 0;

        AthleteStats(Athlete athlete) {
            this.athlete = athlete;
        }
    }

    // ── Elimination bout methods ──────────────────────────────────────────────

    @Override
    @Transactional
    public BoutResponse assignRefereeToEliminationBout(String organizerEmail, Long boutId, AssignRefereeRequest request) {
        Bout bout = getBout(boutId);
        User referee = userRepository.findById(request.refereeUserId())
                .orElseThrow(() -> new IllegalArgumentException("Referee not found: " + request.refereeUserId()));
        if (!bout.getReferees().contains(referee)) {
            bout.getReferees().add(referee);
            boutRepository.save(bout);

            // Notify referee of assignment
            String message = String.format("Fuiste asignado/a como árbitro para el asalto de %s en el torneo '%s'.",
                    getBoutLabel(bout), bout.getTournament().getName());
            notificationService.sendNotification(
                    referee.getId(),
                    bout.getTournament().getId(),
                    bout.getId(),
                    NotificationType.REFEREE_ASSIGNMENT,
                    message
            );
        }
        return toResponse(bout);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoutResponse> getEliminationBouts(Long tournamentId) {
        return boutRepository.findByTournamentIdAndPouleIsNullOrderByEliminationRoundAscBracketPositionAsc(tournamentId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoutResponse> getMyEliminationBouts(String refereeEmail, Long tournamentId) {
        User referee = userRepository.findByEmail(refereeEmail)
                .orElseThrow(() -> new IllegalArgumentException("Referee not found: " + refereeEmail));
        return boutRepository.findByTournamentIdAndPouleIsNullAndRefereesId(tournamentId, referee.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoutResponse> getMyAssignedBouts(String refereeEmail, Long tournamentId) {
        User referee = userRepository.findByEmail(refereeEmail)
                .orElseThrow(() -> new UserNotFoundException(refereeEmail));
        return boutRepository.findAssignedBouts(tournamentId, referee.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BoutResponse removeRefereeFromEliminationBout(String organizerEmail, Long boutId, Long refereeUserId) {
        Bout bout = getBout(boutId);
        
        // Notify before removing
        bout.getReferees().stream()
                .filter(r -> r.getId().equals(refereeUserId))
                .findFirst()
                .ifPresent(referee -> {
                    String message = String.format("Fuiste removido/a como árbitro del asalto de %s en el torneo '%s'.",
                            getBoutLabel(bout), bout.getTournament().getName());
                    notificationService.sendNotification(
                            referee.getId(),
                            bout.getTournament().getId(),
                            bout.getId(),
                            NotificationType.REFEREE_ASSIGNMENT,
                            message
                    );
                });

        bout.getReferees().removeIf(r -> r.getId().equals(refereeUserId));
        return toResponse(boutRepository.save(bout));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveBoutSummary> getLiveBouts() {
        return boutRepository.findByStatusOrderByStartedAtDesc(BoutStatus.IN_PROGRESS)
                .stream()
                .map(b -> new LiveBoutSummary(
                        b.getId(),
                        b.getTournament().getId(),
                        b.getTournament().getName(),
                        b.getPiste(),
                        b.getStatus(),
                        b.getAthleteLeft() != null
                                ? b.getAthleteLeft().getFirstName() + " " + b.getAthleteLeft().getLastName()
                                : "BYE",
                        b.getAthleteRight() != null
                                ? b.getAthleteRight().getFirstName() + " " + b.getAthleteRight().getLastName()
                                : "BYE",
                        b.getScoreLeft(),
                        b.getScoreRight(),
                        b.getElapsedSeconds(),
                        b.getPoule() != null ? b.getPoule().getId() : null,
                        b.getPoule() != null ? b.getPoule().getNumber() : null,
                        b.getEliminationRound()
                ))
                .collect(Collectors.toList());
    }

    private String getBoutLabel(Bout bout) {
        if (bout.getEliminationRound() == null) {
            return "Eliminatoria";
        }
        String roundName = switch (bout.getEliminationRound()) {
            case ROUND_OF_64 -> "Tabla de 64";
            case ROUND_OF_32 -> "Tabla de 32";
            case ROUND_OF_16 -> "Octavos de final";
            case QUARTERFINAL -> "Cuartos de final";
            case SEMIFINAL -> "Semifinal";
            case FINAL -> "Final";
        };
        String left = bout.getAthleteLeft() != null ? bout.getAthleteLeft().getLastName() : "A confirmar";
        String right = bout.getAthleteRight() != null ? bout.getAthleteRight().getLastName() : "A confirmar";
        return String.format("%s (%s vs %s)", roundName, left, right);
    }
}
