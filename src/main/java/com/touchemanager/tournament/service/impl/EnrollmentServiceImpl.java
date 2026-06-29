package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.athlete.entity.AthleteDocument;
import com.touchemanager.athlete.repository.AthleteDocumentRepository;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.shared.exception.AthleteNotFoundException;
import com.touchemanager.tournament.dto.EnrollmentRequest;
import com.touchemanager.tournament.dto.EnrollmentResponse;
import com.touchemanager.tournament.entity.Enrollment;
import com.touchemanager.tournament.entity.EnrollmentStatus;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.service.EnrollmentService;
import com.touchemanager.tournament.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final TournamentRepository tournamentRepository;
    private final AthleteRepository athleteRepository;
    private final AthleteDocumentRepository athleteDocumentRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public EnrollmentResponse enroll(String email, EnrollmentRequest request) {
        Athlete athlete = getAthleteByEmail(email);

        Tournament tournament = tournamentRepository.findById(request.tournamentId())
                .orElseThrow(() -> new IllegalArgumentException("Torneo no encontrado con ID: " + request.tournamentId()));

        // 1. Validate athlete documentation is complete (Apto Médico y Comprobante cargados)
        List<AthleteDocument> documents = athleteDocumentRepository.findByAthleteId(athlete.getId());
        boolean hasMedical = documents.stream().anyMatch(d -> d.getDocumentType() == com.touchemanager.athlete.entity.DocumentType.MEDICAL_CLEARANCE);
        boolean hasPayment = documents.stream().anyMatch(d -> d.getDocumentType() == com.touchemanager.athlete.entity.DocumentType.PAYMENT_RECEIPT);

        if (!hasMedical || !hasPayment) {
            throw new IllegalArgumentException("Documentación incompleta: Debes subir tu Apto Médico y tu Comprobante de Pago en tu perfil antes de inscribirte.");
        }

        // 2. Validate gender and category eligibility
        if (athlete.getGender() != tournament.getGender()) {
            throw new IllegalArgumentException("Este torneo es para la categoría de género " + tournament.getGender() + " y no coincide con tu perfil.");
        }
        com.touchemanager.tournament.entity.Category athleteCategory = deriveCategory(athlete.getBirthDate());
        Set<com.touchemanager.tournament.entity.Category> eligible = getEligibleCategories(athleteCategory);
        if (!eligible.contains(tournament.getCategory())) {
            throw new IllegalArgumentException("No cumplís los requisitos de categoría para este torneo.");
        }

        // 2. Check if already enrolled or has cancelled pre-enrollment
        Optional<Enrollment> existingEnrollmentOpt = enrollmentRepository.findByAthleteIdAndTournamentId(athlete.getId(), tournament.getId());
        
        LocalDate date = tournament.getDate();
        LocalDate today = LocalDate.now();
        LocalDate regularDeadline = date.minusWeeks(1).with(DayOfWeek.WEDNESDAY);
        LocalDate lateDeadline = date.minusDays(8);

        if (existingEnrollmentOpt.isPresent()) {
            Enrollment existing = existingEnrollmentOpt.get();
            
            if (existing.getStatus() == EnrollmentStatus.PAID) {
                throw new IllegalArgumentException("Ya estás inscrito en este torneo");
            }
            
            if (existing.getStatus() == EnrollmentStatus.PENDING_PAYMENT) {
                // If it is pending payment, return it so they can proceed to pay
                return mapToResponse(existing);
            }
            
            if (existing.getStatus() == EnrollmentStatus.CANCELLED) {
                // If it was already PAID in the past, reactivate with NO cost (re-use the same PAID amount, no checkout needed)
                if (existing.getPaymentId() != null) {
                    existing.setStatus(EnrollmentStatus.PAID);
                    existing.setEnrollmentDate(LocalDateTime.now());
                    Enrollment saved = enrollmentRepository.save(existing);
                    return mapToResponse(saved);
                } else {
                    // Never paid: check deadlines and recalculate price
                    if (today.isAfter(lateDeadline)) {
                        throw new IllegalArgumentException("Las inscripciones para este torneo están cerradas.");
                    }
                    
                    BigDecimal finalPrice;
                    if (today.isAfter(regularDeadline)) {
                        finalPrice = tournament.getBasePrice().multiply(BigDecimal.valueOf(1.5));
                    } else {
                        finalPrice = tournament.getBasePrice();
                    }
                    
                    existing.setStatus(EnrollmentStatus.PENDING_PAYMENT);
                    existing.setAmount(finalPrice);
                    existing.setEnrollmentDate(LocalDateTime.now());
                    Enrollment saved = enrollmentRepository.save(existing);
                    return mapToResponse(saved);
                }
            }
        }

        // 3. New Enrollment: Calculate price and check closed status
        if (today.isAfter(lateDeadline)) {
            throw new IllegalArgumentException("Las inscripciones para este torneo están cerradas.");
        }

        BigDecimal finalPrice;
        if (today.isAfter(regularDeadline)) {
            // Late registration fee (+50%)
            finalPrice = tournament.getBasePrice().multiply(BigDecimal.valueOf(1.5));
        } else {
            // Regular price
            finalPrice = tournament.getBasePrice();
        }

        // 4. Save pre-enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setAthlete(athlete);
        enrollment.setTournament(tournament);
        enrollment.setEnrollmentDate(LocalDateTime.now());
        enrollment.setAmount(finalPrice);
        enrollment.setStatus(EnrollmentStatus.PENDING_PAYMENT);

        Enrollment saved = enrollmentRepository.save(enrollment);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public EnrollmentResponse confirmPayment(String email, Long enrollmentId, String paymentId) {
        Athlete athlete = getAthleteByEmail(email);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada con ID: " + enrollmentId));

        if (!enrollment.getAthlete().getId().equals(athlete.getId())) {
            throw new IllegalArgumentException("Acceso denegado: esta inscripción no pertenece a tu perfil.");
        }

        if (enrollment.getStatus() != EnrollmentStatus.PAID) {
            enrollment.setStatus(EnrollmentStatus.PAID);
            enrollment.setPaymentId(paymentId);
            enrollment = enrollmentRepository.save(enrollment);
        }

        return mapToResponse(enrollment);
    }

    @Override
    @Transactional
    public EnrollmentResponse cancelEnrollment(String email, Long enrollmentId) {
        Athlete athlete = getAthleteByEmail(email);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada con ID: " + enrollmentId));

        if (!enrollment.getAthlete().getId().equals(athlete.getId())) {
            throw new IllegalArgumentException("Acceso denegado: esta inscripción no pertenece a tu perfil.");
        }

        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        Enrollment saved = enrollmentRepository.save(enrollment);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public String getPaymentLink(String email, Long enrollmentId) {
        Athlete athlete = getAthleteByEmail(email);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada con ID: " + enrollmentId));

        if (!enrollment.getAthlete().getId().equals(athlete.getId())) {
            throw new IllegalArgumentException("Acceso denegado: esta inscripción no pertenece a tu perfil.");
        }

        return paymentService.createPaymentLink(enrollment);
    }

    private Athlete getAthleteByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return athleteRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AthleteNotFoundException(email));
    }

    private EnrollmentResponse mapToResponse(Enrollment enrollment) {
        String paymentLink = paymentService.createPaymentLink(enrollment);
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getAthlete().getId(),
                enrollment.getTournament().getId(),
                enrollment.getTournament().getName(),
                enrollment.getAmount(),
                enrollment.getStatus(),
                paymentLink
        );
    }

    private com.touchemanager.tournament.entity.Category deriveCategory(LocalDate birthDate) {
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age >= 40) return com.touchemanager.tournament.entity.Category.VETERAN;
        if (age >= 18) return com.touchemanager.tournament.entity.Category.SENIOR;
        if (age >= 16) return com.touchemanager.tournament.entity.Category.JUNIOR;
        if (age >= 14) return com.touchemanager.tournament.entity.Category.CADET;
        if (age >= 12) return com.touchemanager.tournament.entity.Category.PRE_CADET;
        if (age >= 10) return com.touchemanager.tournament.entity.Category.INFANTILE;
        return com.touchemanager.tournament.entity.Category.PRE_INFANTILE;
    }

    private Set<com.touchemanager.tournament.entity.Category> getEligibleCategories(
            com.touchemanager.tournament.entity.Category athleteCategory) {
        return switch (athleteCategory) {
            case PRE_INFANTILE -> EnumSet.of(com.touchemanager.tournament.entity.Category.PRE_INFANTILE,
                    com.touchemanager.tournament.entity.Category.INFANTILE,
                    com.touchemanager.tournament.entity.Category.PRE_CADET,
                    com.touchemanager.tournament.entity.Category.CADET,
                    com.touchemanager.tournament.entity.Category.JUNIOR,
                    com.touchemanager.tournament.entity.Category.SENIOR);
            case INFANTILE -> EnumSet.of(com.touchemanager.tournament.entity.Category.INFANTILE,
                    com.touchemanager.tournament.entity.Category.PRE_CADET,
                    com.touchemanager.tournament.entity.Category.CADET,
                    com.touchemanager.tournament.entity.Category.JUNIOR,
                    com.touchemanager.tournament.entity.Category.SENIOR);
            case PRE_CADET -> EnumSet.of(com.touchemanager.tournament.entity.Category.PRE_CADET,
                    com.touchemanager.tournament.entity.Category.CADET,
                    com.touchemanager.tournament.entity.Category.JUNIOR,
                    com.touchemanager.tournament.entity.Category.SENIOR);
            case CADET -> EnumSet.of(com.touchemanager.tournament.entity.Category.CADET,
                    com.touchemanager.tournament.entity.Category.JUNIOR,
                    com.touchemanager.tournament.entity.Category.SENIOR);
            case JUNIOR -> EnumSet.of(com.touchemanager.tournament.entity.Category.JUNIOR,
                    com.touchemanager.tournament.entity.Category.SENIOR);
            case SENIOR -> EnumSet.of(com.touchemanager.tournament.entity.Category.SENIOR);
            case VETERAN -> EnumSet.of(com.touchemanager.tournament.entity.Category.VETERAN,
                    com.touchemanager.tournament.entity.Category.SENIOR);
        };
    }
}
