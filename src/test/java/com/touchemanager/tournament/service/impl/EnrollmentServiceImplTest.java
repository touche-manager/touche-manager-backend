package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.entity.AthleteDocument;
import com.touchemanager.athlete.entity.DocumentType;
import com.touchemanager.athlete.entity.Gender;
import com.touchemanager.athlete.repository.AthleteDocumentRepository;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.tournament.dto.EnrollmentRequest;
import com.touchemanager.tournament.dto.EnrollmentResponse;
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
import org.mockito.ArgumentCaptor;
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
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private AthleteRepository athleteRepository;
    @Mock
    private AthleteDocumentRepository athleteDocumentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private User testUser;
    private Athlete testAthlete;
    private String testEmail = "athlete@test.com";
    private Tournament regularTournament;
    private Tournament lateTournament;
    private Tournament closedTournament;

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

        regularTournament = new Tournament();
        regularTournament.setId(100L);
        regularTournament.setName("Copa Regular");
        regularTournament.setWeapon(Weapon.FOIL);
        regularTournament.setCategory(Category.SENIOR);
        regularTournament.setGender(Gender.MALE);
        regularTournament.setLocation("Sede Regular");
        regularTournament.setDate(LocalDate.now().plusDays(20));
        regularTournament.setBasePrice(BigDecimal.valueOf(1000.00));

        lateTournament = new Tournament();
        lateTournament.setId(101L);
        lateTournament.setName("Copa Tardia");
        lateTournament.setWeapon(Weapon.EPEE);
        lateTournament.setCategory(Category.SENIOR);
        lateTournament.setGender(Gender.MALE);
        lateTournament.setLocation("Sede Tardia");
        lateTournament.setDate(LocalDate.now().plusDays(8));
        lateTournament.setBasePrice(BigDecimal.valueOf(1000.00));

        closedTournament = new Tournament();
        closedTournament.setId(102L);
        closedTournament.setName("Copa Cerrada");
        closedTournament.setWeapon(Weapon.SABRE);
        closedTournament.setCategory(Category.SENIOR);
        closedTournament.setGender(Gender.MALE);
        closedTournament.setLocation("Sede Cerrada");
        closedTournament.setDate(LocalDate.now().plusDays(4));
        closedTournament.setBasePrice(BigDecimal.valueOf(1000.00));
    }

    private List<AthleteDocument> createCompleteDocuments() {
        AthleteDocument medical = new AthleteDocument();
        medical.setDocumentType(DocumentType.MEDICAL_CLEARANCE);

        AthleteDocument payment = new AthleteDocument();
        payment.setDocumentType(DocumentType.PAYMENT_RECEIPT);

        return List.of(medical, payment);
    }

    @Test
    void enroll_Success_RegularPrice() {
        EnrollmentRequest request = new EnrollmentRequest(regularTournament.getId());

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findById(request.tournamentId())).thenReturn(Optional.of(regularTournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), regularTournament.getId()))
                .thenReturn(Optional.empty());
        when(athleteDocumentRepository.findByAthleteId(testAthlete.getId())).thenReturn(createCompleteDocuments());

        Enrollment savedEnrollment = new Enrollment();
        savedEnrollment.setId(999L);
        savedEnrollment.setAthlete(testAthlete);
        savedEnrollment.setTournament(regularTournament);
        savedEnrollment.setAmount(BigDecimal.valueOf(1000.00));
        savedEnrollment.setStatus(EnrollmentStatus.PENDING_PAYMENT);

        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(savedEnrollment);

        EnrollmentResponse response = enrollmentService.enroll(testEmail, request);

        assertNotNull(response);
        assertEquals(999L, response.id());
        assertEquals(testAthlete.getId(), response.athleteId());
        assertEquals(regularTournament.getId(), response.tournamentId());
        assertEquals(regularTournament.getName(), response.tournamentName());
        assertEquals(BigDecimal.valueOf(1000.00), response.amount());
        assertEquals(EnrollmentStatus.PENDING_PAYMENT, response.status());
        assertEquals("/athlete/enrollments/pay?id=999", response.paymentLink());

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        Enrollment captured = captor.getValue();
        assertEquals(EnrollmentStatus.PENDING_PAYMENT, captured.getStatus());
        assertEquals(0, captured.getAmount().compareTo(BigDecimal.valueOf(1000.00)));
    }

    @Test
    void enroll_Success_LatePrice() {
        LocalDate fixedToday = LocalDate.of(2026, 6, 4);
        LocalDate tournamentDate = LocalDate.of(2026, 6, 12);

        try (MockedStatic<LocalDate> mockedLocalDate = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(fixedToday);

            Tournament customLateTournament = new Tournament();
            customLateTournament.setId(101L);
            customLateTournament.setName("Copa Tardia");
            customLateTournament.setWeapon(Weapon.EPEE);
            customLateTournament.setCategory(Category.SENIOR);
            customLateTournament.setGender(Gender.MALE);
            customLateTournament.setLocation("Sede Tardia");
            customLateTournament.setDate(tournamentDate);
            customLateTournament.setBasePrice(BigDecimal.valueOf(1000.00));


            EnrollmentRequest request = new EnrollmentRequest(customLateTournament.getId());

            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
            when(tournamentRepository.findById(request.tournamentId())).thenReturn(Optional.of(customLateTournament));
            when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), customLateTournament.getId()))
                    .thenReturn(Optional.empty());
            when(athleteDocumentRepository.findByAthleteId(testAthlete.getId())).thenReturn(createCompleteDocuments());

            Enrollment savedEnrollment = new Enrollment();
            savedEnrollment.setId(888L);
            savedEnrollment.setAthlete(testAthlete);
            savedEnrollment.setTournament(customLateTournament);
            savedEnrollment.setAmount(BigDecimal.valueOf(1500.00));
            savedEnrollment.setStatus(EnrollmentStatus.PENDING_PAYMENT);

            when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(savedEnrollment);

            EnrollmentResponse response = enrollmentService.enroll(testEmail, request);

            assertNotNull(response);
            assertEquals(BigDecimal.valueOf(1500.00), response.amount());

            ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
            verify(enrollmentRepository).save(captor.capture());
            Enrollment captured = captor.getValue();
            assertEquals(0, captured.getAmount().compareTo(BigDecimal.valueOf(1500.00))); // $1000 * 1.5
        }
    }

    @Test
    void enroll_Failure_IncompleteDocumentation() {
        EnrollmentRequest request = new EnrollmentRequest(regularTournament.getId());

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findById(request.tournamentId())).thenReturn(Optional.of(regularTournament));

        // Only medical clearance uploaded, payment receipt missing
        AthleteDocument medical = new AthleteDocument();
        medical.setDocumentType(DocumentType.MEDICAL_CLEARANCE);
        when(athleteDocumentRepository.findByAthleteId(testAthlete.getId())).thenReturn(List.of(medical));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            enrollmentService.enroll(testEmail, request);
        });

        assertTrue(exception.getMessage().contains("Documentación incompleta"));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void enroll_Failure_ClosedRegistration() {
        EnrollmentRequest request = new EnrollmentRequest(closedTournament.getId());

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findById(request.tournamentId())).thenReturn(Optional.of(closedTournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), closedTournament.getId()))
                .thenReturn(Optional.empty());
        when(athleteDocumentRepository.findByAthleteId(testAthlete.getId())).thenReturn(createCompleteDocuments());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            enrollmentService.enroll(testEmail, request);
        });

        assertTrue(exception.getMessage().contains("cerradas"));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void enroll_Failure_AlreadyEnrolled() {
        EnrollmentRequest request = new EnrollmentRequest(regularTournament.getId());

        Enrollment existing = new Enrollment();
        existing.setStatus(EnrollmentStatus.PAID);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findById(request.tournamentId())).thenReturn(Optional.of(regularTournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), regularTournament.getId()))
                .thenReturn(Optional.of(existing));
        when(athleteDocumentRepository.findByAthleteId(testAthlete.getId())).thenReturn(createCompleteDocuments());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            enrollmentService.enroll(testEmail, request);
        });

        assertTrue(exception.getMessage().contains("Ya estás inscrito"));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void enroll_ReactivatePaidCancelledEnrollment_ReturnsPaidStatus() {
        EnrollmentRequest request = new EnrollmentRequest(regularTournament.getId());

        Enrollment existing = new Enrollment();
        existing.setId(400L);
        existing.setAthlete(testAthlete);
        existing.setTournament(regularTournament);
        existing.setStatus(EnrollmentStatus.CANCELLED);
        existing.setPaymentId("PREV-PAY-123");
        existing.setAmount(BigDecimal.valueOf(1000.00));

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findById(request.tournamentId())).thenReturn(Optional.of(regularTournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), regularTournament.getId()))
                .thenReturn(Optional.of(existing));
        when(athleteDocumentRepository.findByAthleteId(testAthlete.getId())).thenReturn(createCompleteDocuments());
        when(enrollmentRepository.save(existing)).thenReturn(existing);

        EnrollmentResponse response = enrollmentService.enroll(testEmail, request);

        assertNotNull(response);
        assertEquals(EnrollmentStatus.PAID, response.status());
        assertEquals(BigDecimal.valueOf(1000.00), response.amount());
        verify(enrollmentRepository).save(existing);
        assertEquals(EnrollmentStatus.PAID, existing.getStatus());
    }

    @Test
    void enroll_ReactivateUnpaidCancelledEnrollment_ReturnsPendingStatus() {
        EnrollmentRequest request = new EnrollmentRequest(regularTournament.getId());

        Enrollment existing = new Enrollment();
        existing.setId(400L);
        existing.setAthlete(testAthlete);
        existing.setTournament(regularTournament);
        existing.setStatus(EnrollmentStatus.CANCELLED);
        existing.setPaymentId(null);
        existing.setAmount(BigDecimal.valueOf(1000.00));

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(tournamentRepository.findById(request.tournamentId())).thenReturn(Optional.of(regularTournament));
        when(enrollmentRepository.findByAthleteIdAndTournamentId(testAthlete.getId(), regularTournament.getId()))
                .thenReturn(Optional.of(existing));
        when(athleteDocumentRepository.findByAthleteId(testAthlete.getId())).thenReturn(createCompleteDocuments());
        when(enrollmentRepository.save(existing)).thenReturn(existing);

        EnrollmentResponse response = enrollmentService.enroll(testEmail, request);

        assertNotNull(response);
        assertEquals(EnrollmentStatus.PENDING_PAYMENT, response.status());
        verify(enrollmentRepository).save(existing);
        assertEquals(EnrollmentStatus.PENDING_PAYMENT, existing.getStatus());
    }

    @Test
    void cancelEnrollment_Success() {
        Long enrollmentId = 555L;

        Enrollment enrollment = new Enrollment();
        enrollment.setId(enrollmentId);
        enrollment.setAthlete(testAthlete);
        enrollment.setTournament(regularTournament);
        enrollment.setStatus(EnrollmentStatus.PAID);
        enrollment.setPaymentId("PAY-123");

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        EnrollmentResponse response = enrollmentService.cancelEnrollment(testEmail, enrollmentId);

        assertNotNull(response);
        assertEquals(EnrollmentStatus.CANCELLED, response.status());
        verify(enrollmentRepository).save(enrollment);
        assertEquals(EnrollmentStatus.CANCELLED, enrollment.getStatus());
    }

    @Test
    void confirmPayment_Success() {
        Long enrollmentId = 555L;
        String paymentId = "mp-payment-123456";

        Enrollment enrollment = new Enrollment();
        enrollment.setId(enrollmentId);
        enrollment.setAthlete(testAthlete);
        enrollment.setTournament(regularTournament);
        enrollment.setAmount(BigDecimal.valueOf(1000.00));
        enrollment.setStatus(EnrollmentStatus.PENDING_PAYMENT);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        Enrollment savedEnrollment = new Enrollment();
        savedEnrollment.setId(enrollmentId);
        savedEnrollment.setAthlete(testAthlete);
        savedEnrollment.setTournament(regularTournament);
        savedEnrollment.setAmount(BigDecimal.valueOf(1000.00));
        savedEnrollment.setStatus(EnrollmentStatus.PAID);
        savedEnrollment.setPaymentId(paymentId);

        when(enrollmentRepository.save(enrollment)).thenReturn(savedEnrollment);

        EnrollmentResponse response = enrollmentService.confirmPayment(testEmail, enrollmentId, paymentId);

        assertNotNull(response);
        assertEquals(EnrollmentStatus.PAID, response.status());
        verify(enrollmentRepository).save(enrollment);
        assertEquals(EnrollmentStatus.PAID, enrollment.getStatus());
        assertEquals(paymentId, enrollment.getPaymentId());
    }

    @Test
    void confirmPayment_Failure_WrongAthlete() {
        Long enrollmentId = 555L;
        String paymentId = "mp-payment-123456";

        Athlete otherAthlete = new Athlete();
        otherAthlete.setId(99L); // Different athlete ID

        Enrollment enrollment = new Enrollment();
        enrollment.setId(enrollmentId);
        enrollment.setAthlete(otherAthlete); // Belongs to other athlete
        enrollment.setTournament(regularTournament);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(athleteRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testAthlete));
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            enrollmentService.confirmPayment(testEmail, enrollmentId, paymentId);
        });

        assertTrue(exception.getMessage().contains("Acceso denegado"));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }
}
