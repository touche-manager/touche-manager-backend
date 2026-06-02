package com.touchemanager.bout.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.bout.dto.BoutEventRequest;
import com.touchemanager.bout.dto.BoutEventResponse;
import com.touchemanager.bout.dto.BoutRequest;
import com.touchemanager.bout.dto.BoutResponse;
import com.touchemanager.bout.dto.TournamentStandingsResponse;
import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.entity.BoutEvent;
import com.touchemanager.bout.entity.BoutFormat;
import com.touchemanager.bout.entity.BoutStatus;
import com.touchemanager.bout.entity.EventSide;
import com.touchemanager.bout.entity.EventType;
import com.touchemanager.bout.repository.BoutEventRepository;
import com.touchemanager.bout.repository.BoutRepository;
import com.touchemanager.bout.service.BoutService;
import com.touchemanager.shared.exception.BoutNotFoundException;
import com.touchemanager.shared.exception.TournamentNotFoundException;
import com.touchemanager.tournament.dto.OrganizerTournamentResponse;
import com.touchemanager.tournament.entity.Enrollment;
import com.touchemanager.tournament.entity.EnrollmentStatus;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoutServiceImpl implements BoutService {

    private final BoutRepository boutRepository;
    private final BoutEventRepository boutEventRepository;
    private final TournamentRepository tournamentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AthleteRepository athleteRepository;

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
        bout.setRefereeEmail(email);

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

        return toResponse(boutRepository.save(bout));
    }

    @Override
    @Transactional
    public BoutResponse recordEvent(String email, Long boutId, BoutEventRequest request) {
        Bout bout = getBout(boutId);

        if (bout.getStatus() != BoutStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Bout is not in progress");
        }

        int scoreDelta = resolveScoreDelta(request.getEventType());

        // Create event
        BoutEvent event = new BoutEvent();
        event.setBout(bout);
        event.setSide(request.getSide());
        event.setEventType(request.getEventType());
        event.setScoreDelta(scoreDelta);
        event.setRecordedAt(LocalDateTime.now());
        event.setRefereeEmail(email);
        boutEventRepository.save(event);

        // Update score on the scoring side
        if (request.getSide() == EventSide.LEFT) {
            bout.setScoreLeft(bout.getScoreLeft() + scoreDelta);
        } else {
            bout.setScoreRight(bout.getScoreRight() + scoreDelta);
        }

        // Check if target score is reached — auto-finish for poule format
        int target = touchesTarget(bout.getFormat());
        if (bout.getFormat() == BoutFormat.POULE &&
                (bout.getScoreLeft() >= target || bout.getScoreRight() >= target)) {
            declareWinner(bout);
        }

        return toResponse(boutRepository.save(bout));
    }

    @Override
    @Transactional
    public BoutResponse updateElapsedTime(String email, Long boutId, int elapsedSeconds) {
        Bout bout = getBout(boutId);

        if (bout.getStatus() != BoutStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Bout is not in progress");
        }

        bout.setElapsedSeconds(elapsedSeconds);
        return toResponse(boutRepository.save(bout));
    }

    @Override
    @Transactional
    public BoutResponse finishBout(String email, Long boutId) {
        Bout bout = getBout(boutId);

        if (bout.getStatus() == BoutStatus.FINISHED) {
            throw new IllegalArgumentException("Bout is already finished");
        }

        declareWinner(bout);
        return toResponse(boutRepository.save(bout));
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
        // Tie: winner stays null (can be decided externally by referee)
    }

    private int resolveScoreDelta(EventType eventType) {
        return switch (eventType) {
            case TOUCHE, PENALTY -> 1;
            case CARD -> 0;  // Yellow card — no points; red card should be handled as PENALTY
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

        return new BoutResponse(
                bout.getId(),
                bout.getTournament().getId(),
                bout.getTournament().getName(),
                bout.getFormat(),
                bout.getStatus(),
                toAthleteSummary(bout.getAthleteLeft()),
                toAthleteSummary(bout.getAthleteRight()),
                bout.getScoreLeft(),
                bout.getScoreRight(),
                bout.getCurrentPeriod(),
                maxPeriods(bout.getFormat()),
                touchesTarget(bout.getFormat()),
                bout.getElapsedSeconds(),
                bout.getWinner() != null ? bout.getWinner().getId() : null,
                bout.getStartedAt(),
                bout.getFinishedAt(),
                eventResponses
        );
    }

    private BoutResponse.AthleteSummary toAthleteSummary(Athlete athlete) {
        return new BoutResponse.AthleteSummary(
                athlete.getId(),
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
}
