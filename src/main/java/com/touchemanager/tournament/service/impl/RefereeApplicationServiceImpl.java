package com.touchemanager.tournament.service.impl;

import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.shared.exception.TournamentNotFoundException;
import com.touchemanager.shared.exception.UserNotFoundException;
import com.touchemanager.tournament.dto.RefereeApplicationResponse;
import com.touchemanager.tournament.dto.ReviewApplicationRequest;
import com.touchemanager.tournament.entity.RefereeApplication;
import com.touchemanager.tournament.entity.RefereeApplicationStatus;
import com.touchemanager.tournament.entity.Tournament;
import com.touchemanager.tournament.repository.RefereeApplicationRepository;
import com.touchemanager.tournament.repository.TournamentRepository;
import com.touchemanager.tournament.service.RefereeApplicationService;
import com.touchemanager.notification.service.NotificationService;
import com.touchemanager.notification.entity.NotificationType;
import com.touchemanager.notification.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefereeApplicationServiceImpl implements RefereeApplicationService {

    private final RefereeApplicationRepository applicationRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public RefereeApplicationResponse apply(String refereeEmail, Long tournamentId) {
        User referee = getUserByEmail(refereeEmail);
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (applicationRepository.existsByRefereeIdAndTournamentId(referee.getId(), tournamentId)) {
            throw new IllegalStateException("Ya te postulaste para arbitrar en este torneo.");
        }

        RefereeApplication application = new RefereeApplication();
        application.setReferee(referee);
        application.setTournament(tournament);
        application.setStatus(RefereeApplicationStatus.PENDING);
        application.setAppliedAt(LocalDateTime.now());
        RefereeApplication saved = applicationRepository.save(application);

        // Notify tournament organizer
        notificationService.sendNotification(
                tournament.getCreatedBy().getId(),
                tournament.getId(),
                null,
                NotificationType.REFEREE_REQUEST,
                String.format("El árbitro %s solicitó arbitrar en tu torneo '%s'.",
                        referee.getEmail(), tournament.getName())
        );

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefereeApplicationResponse> getApplicationsForTournament(String organizerEmail, Long tournamentId) {
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        return applicationRepository.findByTournamentIdOrderByAppliedAtDesc(tournamentId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefereeApplicationResponse> getMyApplications(String refereeEmail) {
        User referee = getUserByEmail(refereeEmail);
        return applicationRepository.findByRefereeIdOrderByAppliedAtDesc(referee.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RefereeApplicationResponse reviewApplication(String organizerEmail, Long applicationId,
                                                        ReviewApplicationRequest request) {
        RefereeApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Postulacion no encontrada: " + applicationId));

        if (request.status() == RefereeApplicationStatus.PENDING) {
            throw new IllegalArgumentException("No se puede volver al estado PENDING.");
        }
        application.setStatus(request.status());
        application.setReviewedAt(LocalDateTime.now());
        RefereeApplication saved = applicationRepository.save(application);

        // Notify referee of confirmation
        String statusStr = saved.getStatus() == RefereeApplicationStatus.ACCEPTED ? "aceptada" : "rechazada";
        notificationService.sendNotification(
                saved.getReferee().getId(),
                saved.getTournament().getId(),
                null,
                NotificationType.REFEREE_CONFIRMATION,
                String.format("Tu solicitud para arbitrar en el torneo '%s' fue %s.",
                        saved.getTournament().getName(), statusStr)
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelApplication(String refereeEmail, Long applicationId) {
        User referee = getUserByEmail(refereeEmail);
        RefereeApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Postulacion no encontrada: " + applicationId));
        if (!application.getReferee().getId().equals(referee.getId())) {
            throw new IllegalArgumentException("No tenes permisos para cancelar esta postulacion.");
        }
        applicationRepository.delete(application);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    /**
     * Maps a RefereeApplication to its response DTO.
     * NOTE: User has no firstName/lastName fields; email is used as the display name.
     */
    private RefereeApplicationResponse toResponse(RefereeApplication a) {
        User ref = a.getReferee();
        Tournament t = a.getTournament();
        return new RefereeApplicationResponse(
                a.getId(),
                t.getId(),
                t.getName(),
                ref.getId(),
                ref.getEmail(),   // User entity has no firstName/lastName — email used as display name
                ref.getEmail(),
                a.getStatus(),
                a.getAppliedAt(),
                a.getReviewedAt()
        );
    }
}
