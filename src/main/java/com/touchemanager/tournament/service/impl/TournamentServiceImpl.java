package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.shared.exception.AthleteNotFoundException;
import com.touchemanager.tournament.dto.PublicTournamentResponse;
import com.touchemanager.tournament.dto.TournamentResponse;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Enrollment;
import com.touchemanager.tournament.entity.EnrollmentStatus;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.entity.TournamentPhase;
import com.touchemanager.tournament.entity.Weapon;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.repository.TournamentSpecification;
import com.touchemanager.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

        // Derive athlete's category from birthDate and build eligible set
        Category athleteCategory = deriveCategory(athlete.getBirthDate());
        Set<Category> eligible = getEligibleCategories(athleteCategory);

        return tournaments.stream()
                .filter(t -> t.getGender() == athlete.getGender())     // only matching gender
                .filter(t -> eligible.contains(t.getCategory()))       // only eligible categories
                .map(t -> mapToResponse(t, athlete))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicTournamentResponse> searchPublicTournaments(
            TournamentPhase status, Weapon weapon, Category category, Gender gender,
            LocalDate dateFrom, LocalDate dateTo) {
        return tournamentRepository
                .findAll(TournamentSpecification.publicSearch(status, category, gender, weapon, dateFrom, dateTo))
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .map(t -> new PublicTournamentResponse(
                        t.getId(), t.getName(), t.getWeapon(), t.getCategory(), t.getGender(),
                        t.getLocation(), t.getDate(), t.getPhase(), t.isNational()))
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

        // Detect cancelled enrollments that were previously paid (eligible for free re-enrollment)
        boolean wasPreviouslyPaid = enrollmentOpt
                .filter(e -> e.getStatus() == EnrollmentStatus.CANCELLED)
                .map(e -> e.getPaymentId() != null)
                .orElse(false);

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
                enrollmentId,
                wasPreviouslyPaid
        );
    }

    /**
     * Derives the athlete's competition category from their birthDate.
     * Age brackets follow standard Argentine fencing federation rules.
     */
    private Category deriveCategory(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age >= 40) return Category.VETERAN;
        if (age >= 18) return Category.SENIOR;
        if (age >= 16) return Category.JUNIOR;
        if (age >= 14) return Category.CADET;
        if (age >= 12) return Category.PRE_CADET;
        if (age >= 10) return Category.INFANTILE;
        return Category.PRE_INFANTILE;
    }

    /**
     * Returns the set of tournament categories an athlete of the given category
     * is eligible to enter. Athletes can always compete up in age; Veterans can
     * only enter Veteran or Senior tournaments.
     */
    private Set<Category> getEligibleCategories(Category athleteCategory) {
        return switch (athleteCategory) {
            case PRE_INFANTILE -> EnumSet.of(Category.PRE_INFANTILE, Category.INFANTILE,
                    Category.PRE_CADET, Category.CADET, Category.JUNIOR, Category.SENIOR);
            case INFANTILE     -> EnumSet.of(Category.INFANTILE, Category.PRE_CADET,
                    Category.CADET, Category.JUNIOR, Category.SENIOR);
            case PRE_CADET     -> EnumSet.of(Category.PRE_CADET, Category.CADET,
                    Category.JUNIOR, Category.SENIOR);
            case CADET         -> EnumSet.of(Category.CADET, Category.JUNIOR, Category.SENIOR);
            case JUNIOR        -> EnumSet.of(Category.JUNIOR, Category.SENIOR);
            case SENIOR        -> EnumSet.of(Category.SENIOR);
            case VETERAN       -> EnumSet.of(Category.VETERAN, Category.SENIOR);
        };
    }
}
