package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.shared.exception.AthleteNotFoundException;
import com.touchemanager.tournament.dto.TournamentResponse;
import com.touchemanager.tournament.entity.Enrollment;
import com.touchemanager.tournament.entity.EnrollmentStatus;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AthleteRepository athleteRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TournamentResponse> getAvailableTournaments(String email) {
        Athlete athlete = getAthleteByEmail(email);
        List<Tournament> tournaments = tournamentRepository.findAllByOrderByDateAsc();

        return tournaments.stream()
                .map(t -> mapToResponse(t, athlete))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentResponse getTournamentDetails(String email, Long tournamentId) {
        Athlete athlete = getAthleteByEmail(email);
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found with ID: " + tournamentId));

        return mapToResponse(tournament, athlete);
    }

    private Athlete getAthleteByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));
    }

    private TournamentResponse mapToResponse(Tournament tournament, Athlete athlete) {
        LocalDate date = tournament.getDate();
        LocalDate today = LocalDate.now();

        // 1. Regular deadline: Wednesday of the week prior to the tournament
        LocalDate regularDeadline = date.minusWeeks(1).with(DayOfWeek.WEDNESDAY);

        // 2. Late deadline: 8 days before the tournament
        LocalDate lateDeadline = date.minusDays(8);

        // Calculate enrollment status and price
        String enrollmentStatus;
        BigDecimal currentPrice;

        if (today.isAfter(lateDeadline)) {
            enrollmentStatus = "CLOSED";
            currentPrice = tournament.getBasePrice();
        } else if (today.isAfter(regularDeadline)) {
            enrollmentStatus = "OPEN_LATE";
            currentPrice = tournament.getBasePrice().multiply(BigDecimal.valueOf(1.5));
        } else {
            enrollmentStatus = "OPEN_REGULAR";
            currentPrice = tournament.getBasePrice();
        }

        // Check if athlete is already enrolled (excluding CANCELLED enrollments)
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByAthleteIdAndTournamentId(athlete.getId(), tournament.getId());
        boolean alreadyEnrolled = enrollmentOpt.isPresent() && enrollmentOpt.get().getStatus() != EnrollmentStatus.CANCELLED;
        String enrollmentStatusLabel = alreadyEnrolled ? enrollmentOpt.get().getStatus().name() : null;
        Long enrollmentId = alreadyEnrolled ? enrollmentOpt.get().getId() : null;

        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getWeapon(),
                tournament.getCategory(),
                tournament.getGender(),
                tournament.getLocation(),
                tournament.getDate(),
                tournament.getBasePrice(),
                regularDeadline,
                lateDeadline,
                currentPrice,
                enrollmentStatus,
                alreadyEnrolled,
                enrollmentStatusLabel,
                enrollmentId
        );
    }
}
