package com.touchemanager.tournament.service.impl;

import com.touchemanager.athlete.entity.AthleteDocument;
import com.touchemanager.athlete.repository.AthleteDocumentRepository;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.shared.exception.DocumentNotFoundException;
import com.touchemanager.shared.exception.TournamentNotFoundException;
import com.touchemanager.shared.exception.TournamentNotOwnedException;
import com.touchemanager.shared.exception.UserNotFoundException;
import com.touchemanager.tournament.dto.DocumentValidationRequest;
import com.touchemanager.tournament.dto.EnrollmentDetailResponse;
import com.touchemanager.tournament.dto.OrganizerTournamentResponse;
import com.touchemanager.tournament.dto.TournamentRequest;
import com.touchemanager.tournament.entity.Enrollment;
import com.touchemanager.tournament.entity.EnrollmentStatus;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.service.OrganizerTournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizerTournamentServiceImpl implements OrganizerTournamentService {

    private final TournamentRepository tournamentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AthleteDocumentRepository athleteDocumentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrganizerTournamentResponse createTournament(String email, TournamentRequest request) {
        User organizer = getUserByEmail(email);

        Tournament tournament = new Tournament();
        tournament.setName(request.getName());
        tournament.setWeapon(request.getWeapon());
        tournament.setCategory(request.getCategory());
        tournament.setGender(request.getGender());
        tournament.setLocation(request.getLocation());
        tournament.setDate(request.getDate());
        tournament.setBasePrice(request.getBasePrice());
        tournament.setCreatedBy(organizer);
        tournament.setNational(request.isNational());

        Tournament saved = tournamentRepository.save(tournament);
        return toResponse(saved, List.of());
    }

    @Override
    @Transactional
    public OrganizerTournamentResponse updateTournament(String email, Long tournamentId, TournamentRequest request) {
        User organizer = getUserByEmail(email);
        Tournament tournament = getOwnedTournament(organizer, tournamentId);

        tournament.setName(request.getName());
        tournament.setWeapon(request.getWeapon());
        tournament.setCategory(request.getCategory());
        tournament.setGender(request.getGender());
        tournament.setLocation(request.getLocation());
        tournament.setDate(request.getDate());
        tournament.setBasePrice(request.getBasePrice());
        tournament.setNational(request.isNational());

        Tournament saved = tournamentRepository.save(tournament);
        List<Enrollment> enrollments = enrollmentRepository.findByTournamentId(saved.getId());
        return toResponse(saved, enrollments);
    }

    @Override
    @Transactional
    public void deleteTournament(String email, Long tournamentId) {
        User organizer = getUserByEmail(email);
        Tournament tournament = getOwnedTournament(organizer, tournamentId);
        tournamentRepository.delete(tournament);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizerTournamentResponse> getMyTournaments(String email) {
        User organizer = getUserByEmail(email);
        List<Tournament> tournaments = tournamentRepository.findByCreatedByIdOrderByDateAsc(organizer.getId());

        return tournaments.stream()
                .map(t -> {
                    List<Enrollment> enrollments = enrollmentRepository.findByTournamentId(t.getId());
                    return toResponse(t, enrollments);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizerTournamentResponse getTournamentById(String email, Long tournamentId) {
        User organizer = getUserByEmail(email);
        Tournament tournament = getOwnedTournament(organizer, tournamentId);
        List<Enrollment> enrollments = enrollmentRepository.findByTournamentId(tournamentId);
        return toResponse(tournament, enrollments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentDetailResponse> getTournamentEnrollments(String email, Long tournamentId) {
        User organizer = getUserByEmail(email);
        getOwnedTournament(organizer, tournamentId);

        List<Enrollment> enrollments = enrollmentRepository.findByTournamentId(tournamentId);

        return enrollments.stream()
                .map(this::toEnrollmentDetail)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void validateDocument(String email, Long documentId, DocumentValidationRequest request) {
        // Verify organizer exists
        getUserByEmail(email);

        AthleteDocument document = athleteDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        document.setValidationStatus(request.getValidationStatus());
        document.setReviewNotes(request.getReviewNotes());
        athleteDocumentRepository.save(document);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    private Tournament getOwnedTournament(User organizer, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        // ADMIN can manage any tournament; ORGANIZER only their own
        boolean isAdmin = organizer.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ADMIN"));

        if (!isAdmin && (tournament.getCreatedBy() == null
                || !tournament.getCreatedBy().getId().equals(organizer.getId()))) {
            throw new TournamentNotOwnedException(tournamentId);
        }
        return tournament;
    }

    private OrganizerTournamentResponse toResponse(Tournament tournament, List<Enrollment> enrollments) {
        long total = enrollments.size();
        long paid = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.PAID).count();
        long pending = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.PENDING_PAYMENT).count();
        long cancelled = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.CANCELLED).count();

        return new OrganizerTournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getWeapon(),
                tournament.getCategory(),
                tournament.getGender(),
                tournament.getLocation(),
                tournament.getDate(),
                tournament.getBasePrice(),
                tournament.getPhase(),
                tournament.getAdvancementRate(),
                tournament.isNational(),
                total,
                paid,
                pending,
                cancelled
        );
    }

    private EnrollmentDetailResponse toEnrollmentDetail(Enrollment enrollment) {
        var athlete = enrollment.getAthlete();
        var athleteInfo = new EnrollmentDetailResponse.AthleteInfo(
                athlete.getId(),
                athlete.getFirstName(),
                athlete.getLastName(),
                athlete.getDni(),
                athlete.getBirthDate(),
                athlete.getClub(),
                athlete.getProvince()
        );

        List<AthleteDocument> docs = athleteDocumentRepository.findByAthleteId(athlete.getId());
        List<EnrollmentDetailResponse.DocumentInfo> documentInfos = docs.stream()
                .map(d -> new EnrollmentDetailResponse.DocumentInfo(
                        d.getId(),
                        d.getDocumentType(),
                        d.getFileKey(),
                        d.getValidationStatus(),
                        d.getReviewNotes(),
                        d.getUploadDate()
                ))
                .collect(Collectors.toList());

        return new EnrollmentDetailResponse(
                enrollment.getId(),
                enrollment.getStatus(),
                enrollment.getAmount(),
                enrollment.getEnrollmentDate(),
                athleteInfo,
                documentInfos
        );
    }
}
