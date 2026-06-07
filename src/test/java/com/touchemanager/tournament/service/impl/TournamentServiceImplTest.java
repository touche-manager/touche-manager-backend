package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.tournament.dto.TournamentResponse;
import com.touchemanager.tournament.entity.Category;
import com.touchemanager.tournament.entity.Enrollment;
import com.touchemanager.tournament.entity.EnrollmentStatus;
import com.touchemanager.tournament.entity.Tournament;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private String testEmail = "athlete@test.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(testEmail);

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

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.empty());

        List<TournamentResponse> responses = tournamentService.getAvailableTournaments(testEmail);

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

            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
            when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
            when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                    .thenReturn(Optional.empty());

            List<TournamentResponse> responses = tournamentService.getAvailableTournaments(testEmail);

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

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.empty());

        List<TournamentResponse> responses = tournamentService.getAvailableTournaments(testEmail);

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

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.of(mockEnrollment));

        List<TournamentResponse> responses = tournamentService.getAvailableTournaments(testEmail);

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

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findAllByOrderByDateAsc()).thenReturn(List.of(tournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), tournament.getId()))
                .thenReturn(Optional.of(mockEnrollment));

        List<TournamentResponse> responses = tournamentService.getAvailableTournaments(testEmail);

        assertEquals(1, responses.size());
        TournamentResponse resp = responses.get(0);
        assertFalse(resp.alreadyEnrolled());
        assertNull(resp.enrollmentStatusLabel());
        assertNull(resp.enrollmentId());
    }
}
