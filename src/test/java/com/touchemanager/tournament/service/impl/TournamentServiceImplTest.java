
package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class TournamentServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AthleteRepository athleteRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TournamentServiceImpl tournamentService;

    private User testUser;
    private Athlete testAthlete;
    private static final String TEST_EMAIL = "athlete@test.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(TEST_EMAIL);

        testAthlete = new Athlete();
        testAthlete.setId(10L);
        testAthlete.setUser(testUser);
        testAthlete.setFirstName("Juan");
        testAthlete.setLastName("Perez");
        testAthlete.setGender(Gender.MALE);
        testAthlete.setBirthDate(LocalDate.now().minusYears(25)); // 25 years old -> SENIOR
    }

    private Tournament makeTournament(long id, String name, Weapon weapon, Category cat,
                                      Gender gender, String location, LocalDate date, BigDecimal price) {
        Tournament t = new Tournament();
        t.setId(id);
        t.setName(name);
        t.setWeapon(weapon);
        t.setCategory(cat);
        t.setGender(gender);
        t.setLocation(location);
        t.setDate(date);
        t.setBasePrice(price);
        return t;
    }

    @Test
    void getAvailableTournaments_RegularPricingPeriod_ReturnsBasePrice() {
        Tournament tournament = makeTournament(100L, "Copa Test", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Club de Prueba", LocalDate.now().plusDays(15), BigDecimal.valueOf(1000.00));

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.empty());

        List<TournamentResponse> responses = tournamentService.getAvailableTournaments(TEST_EMAIL);

        assertEquals(1, responses.size());
        TournamentResponse resp = responses.get(0);
        assertEquals("OPEN_REGULAR", resp.enrollmentStatus());
        assertEquals(BigDecimal.valueOf(1000.00), resp.currentPrice());
        assertFalse(resp.alreadyEnrolled());
        assertNull(resp.enrollmentStatusLabel());
        assertNull(resp.enrollmentId());
    }

    @Test
    void getAvailableTournaments_LatePricingPeriod_ReturnsPriceWith50PercentFee() {
        LocalDate fixedToday = LocalDate.of(2026, 6, 4);
        LocalDate tournamentDate = LocalDate.of(2026, 6, 12);

        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(fixedToday);

            Tournament tournament = makeTournament(101L, "Copa Tardia", Weapon.EPEE, Category.SENIOR,
                    Gender.MALE, "Sede Club", tournamentDate, BigDecimal.valueOf(1000.00));

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
            when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
            when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                    .thenReturn(Optional.empty());

            List<TournamentResponse> responses = tournamentService.getAvailableTournaments(TEST_EMAIL);

            assertEquals(1, responses.size());
            TournamentResponse resp = responses.get(0);
            assertEquals("OPEN_LATE", resp.enrollmentStatus());
            assertEquals(0, resp.currentPrice().compareTo(BigDecimal.valueOf(1500.00)));
            assertFalse(resp.alreadyEnrolled());
        }
    }

    @Test
    void getAvailableTournaments_ClosedPricingPeriod_ReturnsClosedStatus() {
        Tournament tournament = makeTournament(102L, "Copa Cerrada", Weapon.SABRE, Category.SENIOR,
                Gender.MALE, "Sede Club 2", LocalDate.now().plusDays(4), BigDecimal.valueOf(1000.00));

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.empty());

        List<TournamentResponse> responses = tournamentService.getAvailableTournaments(TEST_EMAIL);

        assertEquals(1, responses.size());
        TournamentResponse resp = responses.get(0);
        assertEquals("CLOSED", resp.enrollmentStatus());
        assertFalse(resp.alreadyEnrolled());
    }

    @Test
    void getAvailableTournaments_AlreadyEnrolled_ReturnsEnrolledDetails() {
        Tournament tournament = makeTournament(103L, "Copa Inscripto", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede Club 3", LocalDate.now().plusDays(20), BigDecimal.valueOf(1000.00));

        Enrollment mockEnrollment = new Enrollment();
        mockEnrollment.setId(500L);
        mockEnrollment.setAthlete(testAthlete);
        mockEnrollment.setTournament(tournament);
        mockEnrollment.setStatus(EnrollmentStatus.PENDING_PAYMENT);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.of(mockEnrollment));

        List<TournamentResponse> responses = tournamentService.getAvailableTournaments(TEST_EMAIL);

        assertEquals(1, responses.size());
        TournamentResponse resp = responses.get(0);
        assertTrue(resp.alreadyEnrolled());
        assertEquals("PENDING_PAYMENT", resp.enrollmentStatusLabel());
        assertEquals(500L, resp.enrollmentId());
    }

    @Test
    void getAvailableTournaments_CancelledEnrollment_ReturnsNotEnrolled() {
        Tournament tournament = makeTournament(104L, "Copa Cancelada", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede Club 4", LocalDate.now().plusDays(20), BigDecimal.valueOf(1000.00));

        Enrollment mockEnrollment = new Enrollment();
        mockEnrollment.setId(600L);
        mockEnrollment.setAthlete(testAthlete);
        mockEnrollment.setTournament(tournament);
        mockEnrollment.setStatus(EnrollmentStatus.CANCELLED);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.of(mockEnrollment));

        List<TournamentResponse> responses = tournamentService.getAvailableTournaments(TEST_EMAIL);

        assertEquals(1, responses.size());
        TournamentResponse resp = responses.get(0);
        assertFalse(resp.alreadyEnrolled());
        assertNull(resp.enrollmentStatusLabel());
        assertNull(resp.enrollmentId());
    }

    @Test
    void getTournamentDetails_Success() {
        Tournament tournament = makeTournament(105L, "Copa Detalles", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede Detalles", LocalDate.now().plusDays(20), BigDecimal.valueOf(1000.00));

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.empty());

        TournamentResponse response = tournamentService.getTournamentDetails(TEST_EMAIL, tournament.getId());

        assertNotNull(response);
        assertEquals(105L, response.id());
        assertEquals("Copa Detalles", response.name());
        assertEquals(Weapon.FOIL, response.weapon());
        assertEquals(Category.SENIOR, response.category());
        assertEquals(Gender.MALE, response.gender());
        assertEquals("Sede Detalles", response.location());
        assertEquals(BigDecimal.valueOf(1000.00), response.basePrice());
        assertFalse(response.alreadyEnrolled());
    }

    @Test
    void getTournamentDetails_WithEnrollment() {
        Tournament tournament = makeTournament(106L, "Copa Con Inscripción", Weapon.EPEE, Category.SENIOR,
                Gender.MALE, "Sede Inscripción", LocalDate.now().plusDays(20), BigDecimal.valueOf(1000.00));

        Enrollment mockEnrollment = new Enrollment();
        mockEnrollment.setId(700L);
        mockEnrollment.setAthlete(testAthlete);
        mockEnrollment.setTournament(tournament);
        mockEnrollment.setStatus(EnrollmentStatus.PAID);

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findById(tournament.getId())).thenReturn(Optional.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.of(mockEnrollment));

        TournamentResponse response = tournamentService.getTournamentDetails(TEST_EMAIL, tournament.getId());

        assertNotNull(response);
        assertTrue(response.alreadyEnrolled());
        assertEquals("PAID", response.enrollmentStatusLabel());
        assertEquals(700L, response.enrollmentId());
    }

    @Test
    void getTournamentDetails_TournamentNotFound() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                tournamentService.getTournamentDetails(TEST_EMAIL, 999L)
        );
    }

    @Test
    void getTournamentDetails_UserNotFound() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                tournamentService.getTournamentDetails(TEST_EMAIL, 105L)
        );
    }

    @Test
    void getTournamentDetails_AthleteNotFound() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());

        assertThrows(AthleteNotFoundException.class, () ->
                tournamentService.getTournamentDetails(TEST_EMAIL, 105L)
        );
    }

    @Test
    void searchPublicTournaments_NoFilters() {
        Tournament tournament1 = makeTournament(200L, "Copa Publica 1", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede 1", LocalDate.now().plusDays(10), BigDecimal.valueOf(1000.00));
        tournament1.setPhase(TournamentPhase.ENROLLMENT);

        Tournament tournament2 = makeTournament(201L, "Copa Publica 2", Weapon.EPEE, Category.JUNIOR,
                Gender.FEMALE, "Sede 2", LocalDate.now().plusDays(20), BigDecimal.valueOf(1200.00));
        tournament2.setPhase(TournamentPhase.ENROLLMENT);

        when(tournamentRepository.findAll(any(Specification.class))).thenReturn(List.of(tournament2, tournament1));

        List<PublicTournamentResponse> responses = tournamentService.searchPublicTournaments(
                null, null, null, null, null, null);

        assertEquals(2, responses.size());
        assertEquals("Copa Publica 2", responses.get(0).name());
        assertEquals("Copa Publica 1", responses.get(1).name());
    }

    @Test
    void searchPublicTournaments_WithCategoryFilter() {
        Tournament tournament1 = makeTournament(202L, "Copa Senior", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede 1", LocalDate.now().plusDays(10), BigDecimal.valueOf(1000.00));
        tournament1.setPhase(TournamentPhase.ENROLLMENT);

        Tournament tournament2 = makeTournament(203L, "Copa Junior", Weapon.FOIL, Category.JUNIOR,
                Gender.MALE, "Sede 2", LocalDate.now().plusDays(15), BigDecimal.valueOf(800.00));
        tournament2.setPhase(TournamentPhase.ENROLLMENT);

        when(tournamentRepository.findAll(any(Specification.class))).thenReturn(List.of(tournament1));

        List<PublicTournamentResponse> responses = tournamentService.searchPublicTournaments(
                null, null, Category.SENIOR, null, null, null);

        assertEquals(1, responses.size());
        assertEquals("Copa Senior", responses.get(0).name());
        assertEquals(Category.SENIOR, responses.get(0).category());
    }

    @Test
    void searchPublicTournaments_WithWeaponAndGenderFilter() {
        Tournament tournament = makeTournament(204L, "Copa Espada Femenina", Weapon.SABRE, Category.SENIOR,
                Gender.FEMALE, "Sede 1", LocalDate.now().plusDays(12), BigDecimal.valueOf(950.00));
        tournament.setPhase(TournamentPhase.ENROLLMENT);

        when(tournamentRepository.findAll(any(Specification.class))).thenReturn(List.of(tournament));

        List<PublicTournamentResponse> responses = tournamentService.searchPublicTournaments(
                null, Weapon.SABRE, null, Gender.FEMALE, null, null);

        assertEquals(1, responses.size());
        assertEquals("Copa Espada Femenina", responses.get(0).name());
        assertEquals(Weapon.SABRE, responses.get(0).weapon());
        assertEquals(Gender.FEMALE, responses.get(0).gender());
    }

    @Test
    void searchPublicTournaments_WithDateRange() {
        LocalDate startDate = LocalDate.of(2026, 6, 15);
        LocalDate endDate = LocalDate.of(2026, 6, 30);

        Tournament tournament1 = makeTournament(205L, "Copa Junio", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede 1", LocalDate.of(2026, 6, 20), BigDecimal.valueOf(1000.00));
        tournament1.setPhase(TournamentPhase.ENROLLMENT);

        Tournament tournament2 = makeTournament(206L, "Copa Fuera de Rango", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede 2", LocalDate.of(2026, 7, 5), BigDecimal.valueOf(1000.00));
        tournament2.setPhase(TournamentPhase.ENROLLMENT);

        when(tournamentRepository.findAll(any(Specification.class))).thenReturn(List.of(tournament1));

        List<PublicTournamentResponse> responses = tournamentService.searchPublicTournaments(
                null, null, null, null, startDate, endDate);

        assertEquals(1, responses.size());
        assertEquals("Copa Junio", responses.get(0).name());
        assertTrue(responses.get(0).date().isAfter(startDate.minusDays(1)));
        assertTrue(responses.get(0).date().isBefore(endDate.plusDays(1)));
    }

    @Test
    void searchPublicTournaments_SortedByDateDescending() {
        Tournament tournament1 = makeTournament(207L, "Copa A", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede 1", LocalDate.of(2026, 6, 10), BigDecimal.valueOf(1000.00));
        tournament1.setPhase(TournamentPhase.ENROLLMENT);

        Tournament tournament2 = makeTournament(208L, "Copa B", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede 2", LocalDate.of(2026, 6, 20), BigDecimal.valueOf(1000.00));
        tournament2.setPhase(TournamentPhase.ENROLLMENT);

        Tournament tournament3 = makeTournament(209L, "Copa C", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede 3", LocalDate.of(2026, 6, 15), BigDecimal.valueOf(1000.00));
        tournament3.setPhase(TournamentPhase.ENROLLMENT);

        when(tournamentRepository.findAll(any(Specification.class))).thenReturn(List.of(tournament1, tournament2, tournament3));

        List<PublicTournamentResponse> responses = tournamentService.searchPublicTournaments(
                null, null, null, null, null, null);

        assertEquals(3, responses.size());
        assertEquals("Copa B", responses.get(0).name());
        assertEquals("Copa C", responses.get(1).name());
        assertEquals("Copa A", responses.get(2).name());
    }

    @Test
    void searchPublicTournaments_MultipleFilters() {
        Tournament tournament = makeTournament(210L, "Copa Foil Senior Masculino", Weapon.FOIL, Category.SENIOR,
                Gender.MALE, "Sede 1", LocalDate.of(2026, 6, 20), BigDecimal.valueOf(1000.00));
        tournament.setPhase(TournamentPhase.ENROLLMENT);

        when(tournamentRepository.findAll(any(Specification.class))).thenReturn(List.of(tournament));

        List<PublicTournamentResponse> responses = tournamentService.searchPublicTournaments(
                TournamentPhase.ENROLLMENT, Weapon.FOIL, Category.SENIOR, Gender.MALE,
                LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 30));

        assertEquals(1, responses.size());
        PublicTournamentResponse resp = responses.get(0);
        assertEquals("Copa Foil Senior Masculino", resp.name());
        assertEquals(Weapon.FOIL, resp.weapon());
        assertEquals(Category.SENIOR, resp.category());
        assertEquals(Gender.MALE, resp.gender());
    }

    @Test
    void searchPublicTournaments_NoResults() {
        when(tournamentRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<PublicTournamentResponse> responses = tournamentService.searchPublicTournaments(
                TournamentPhase.FINISHED, null, null, null, null, null);

        assertTrue(responses.isEmpty());
    }
}
