package com.touchemanager.auth.service.impl;

import com.touchemanager.athlete.entity.AthleteDocument;
import com.touchemanager.athlete.entity.DocumentValidationStatus;
import com.touchemanager.athlete.repository.AthleteDocumentRepository;
import com.touchemanager.athlete.repository.AthleteRepository;
import com.touchemanager.auth.dto.AdminPendingDocumentResponse;
import com.touchemanager.auth.dto.AdminStatsResponse;
import com.touchemanager.auth.dto.AdminUserResponse;
import com.touchemanager.auth.dto.UpdateUserRoleRequest;
import com.touchemanager.auth.entity.Role;
import com.touchemanager.auth.entity.RoleName;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.RoleRepository;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.auth.service.AdminService;
import com.touchemanager.shared.exception.DocumentNotFoundException;
import com.touchemanager.shared.exception.RoleNotFoundException;
import com.touchemanager.shared.exception.UserNotFoundException;
import com.touchemanager.tournament.dto.DocumentValidationRequest;
import com.touchemanager.tournament.entity.TournamentPhase;
import com.touchemanager.tournament.repository.EnrollmentRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AthleteRepository athleteRepository;
    private final AthleteDocumentRepository athleteDocumentRepository;
    private final TournamentRepository tournamentRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(this::toUserResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("ID " + userId));

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new RoleNotFoundException(request.getRole().name()));

        if (request.getAction() == UpdateUserRoleRequest.Action.REMOVE) {
            if (user.getRoles().size() <= 1 && user.getRoles().stream()
                    .anyMatch(r -> r.getName() == request.getRole())) {
                throw new IllegalArgumentException("Cannot remove the user's last remaining role");
            }
            user.getRoles().removeIf(r -> r.getName() == request.getRole());
        } else {
            user.getRoles().add(role);
        }

        log.info("Admin {} role {} for user {}", request.getAction(), request.getRole(), userId);
        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPendingDocumentResponse> getPendingDocuments() {
        return athleteDocumentRepository
                .findByValidationStatusOrderByUploadDateAsc(DocumentValidationStatus.PENDING)
                .stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminPendingDocumentResponse validateDocument(Long documentId, DocumentValidationRequest request) {
        AthleteDocument document = athleteDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        document.setValidationStatus(request.getValidationStatus());
        document.setReviewNotes(request.getReviewNotes());

        log.info("Admin validated document {} as {}", documentId, request.getValidationStatus());
        return toDocumentResponse(athleteDocumentRepository.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long finishedTournaments = tournamentRepository.countByPhase(TournamentPhase.FINISHED);
        long totalTournaments = tournamentRepository.count();
        return new AdminStatsResponse(
                userRepository.count(),
                athleteRepository.count(),
                totalTournaments,
                totalTournaments - finishedTournaments,
                finishedTournaments,
                enrollmentRepository.count(),
                athleteDocumentRepository.countByValidationStatus(DocumentValidationStatus.PENDING)
        );
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private AdminUserResponse toUserResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .sorted()
                .toList();
        return new AdminUserResponse(user.getId(), user.getEmail(), user.isActive(),
                user.getCreatedAt(), roles);
    }

    private AdminPendingDocumentResponse toDocumentResponse(AthleteDocument d) {
        return new AdminPendingDocumentResponse(
                d.getId(),
                d.getAthlete().getId(),
                d.getAthlete().getFirstName() + " " + d.getAthlete().getLastName(),
                d.getAthlete().getDni(),
                d.getAthlete().getClub(),
                d.getDocumentType(),
                d.getFileKey(),
                d.getDescription(),
                d.getUploadDate()
        );
    }
}
